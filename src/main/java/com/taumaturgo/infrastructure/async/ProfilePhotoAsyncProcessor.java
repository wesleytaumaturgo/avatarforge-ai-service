package com.taumaturgo.infrastructure.async;

import com.taumaturgo.application.dto.ProfilePhotoJobStatus;
import com.taumaturgo.application.events.ProfilePhotoJobEvents;
import com.taumaturgo.domain.models.ProfilePhoto;
import com.taumaturgo.domain.models.ProcessingStatus;
import com.taumaturgo.domain.repositories.ProfilePhotoJobRepository;
import com.taumaturgo.domain.repositories.ProfilePhotoPersistenceRepository;
import com.taumaturgo.domain.repositories.ProfilePhotoStorageRepository;
import com.taumaturgo.infrastructure.rest.StableDiffusionService;
import com.taumaturgo.infrastructure.async.WebhookNotifier;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class ProfilePhotoAsyncProcessor {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ProfilePhotoJobRepository jobRepository;
    private final ProfilePhotoPersistenceRepository persistenceRepository;
    private final ProfilePhotoStorageRepository storageRepository;
    private final StableDiffusionService stableDiffusionService;
    private final ProfilePhotoJobEvents events;
    private final WebhookNotifier webhookNotifier;

    public ProfilePhotoAsyncProcessor(ProfilePhotoJobRepository jobRepository,
                                      ProfilePhotoPersistenceRepository persistenceRepository,
                                      ProfilePhotoStorageRepository storageRepository,
                                      StableDiffusionService stableDiffusionService,
                                      ProfilePhotoJobEvents events,
                                      WebhookNotifier webhookNotifier) {
        this.jobRepository = jobRepository;
        this.persistenceRepository = persistenceRepository;
        this.storageRepository = storageRepository;
        this.stableDiffusionService = stableDiffusionService;
        this.events = events;
        this.webhookNotifier = webhookNotifier;
    }

    public void enqueue(String jobId) {
        executor.submit(() -> process(jobId));
    }

    @Blocking
    void process(String jobId) {
        var optionalJob = jobRepository.findById(jobId);
        if (optionalJob.isEmpty()) {
            return;
        }

        var job = optionalJob.get();
        Path originalPath = Path.of(job.profilePhoto().originalPhoto());
        try {
            jobRepository.updateStatus(jobId, ProcessingStatus.PROCESSING, null);
            publish(jobId);

            var generated = stableDiffusionService.generate(job.profilePhoto()).await().indefinitely();

            var originalS3 = storageRepository.store(job.customerId(), job.profilePhoto()).await().indefinitely();

            var generatedS3 = storageRepository.store(job.customerId(),
                                                      job.profilePhoto(),
                                                      generated).await().indefinitely();

            persistenceRepository.save(job.customerId(),
                                       new ProfilePhoto(job.profilePhoto().id(),
                                                        originalS3,
                                                        generatedS3));
            jobRepository.updateResult(jobId, originalS3, generatedS3);
            jobRepository.updateStatus(jobId, ProcessingStatus.DONE, null);
            publish(jobId);
            jobRepository.findById(jobId)
                         .map(ProfilePhotoJobStatus::fromDomain)
                         .ifPresent(status -> webhookNotifier.notify(job.callbackUrl(), status));
        } catch (Exception exception) {
            Logger.getLogger(getClass()).error("Error processing job %s".formatted(jobId), exception);
            jobRepository.updateStatus(jobId, ProcessingStatus.FAILED, exception.getMessage());
            publish(jobId);
            jobRepository.findById(jobId)
                         .map(ProfilePhotoJobStatus::fromDomain)
                         .ifPresent(status -> webhookNotifier.notify(job.callbackUrl(), status));
        } finally {
            deleteSilently(originalPath);
        }
    }

    private void publish(String jobId) {
        jobRepository.findById(jobId)
                     .map(ProfilePhotoJobStatus::fromDomain)
                     .ifPresent(events::publish);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private void deleteSilently(Path path) {
        try {
            if (path != null) {
                Files.deleteIfExists(path);
            }
        } catch (Exception exception) {
            Logger.getLogger(getClass()).warnf(exception, "Failed to cleanup temp file %s", path);
        }
    }
}
