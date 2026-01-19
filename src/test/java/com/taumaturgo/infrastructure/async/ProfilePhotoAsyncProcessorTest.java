package com.taumaturgo.infrastructure.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taumaturgo.application.dto.ProfilePhotoJobStatus;
import com.taumaturgo.application.events.ProfilePhotoJobEvents;
import com.taumaturgo.domain.models.ProfilePhoto;
import com.taumaturgo.domain.models.ProfilePhotoJob;
import com.taumaturgo.domain.models.ProcessingStatus;
import com.taumaturgo.domain.repositories.ProfilePhotoJobRepository;
import com.taumaturgo.domain.repositories.ProfilePhotoPersistenceRepository;
import com.taumaturgo.domain.repositories.ProfilePhotoStorageRepository;
import com.taumaturgo.infrastructure.rest.StableDiffusionService;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilePhotoAsyncProcessorTest {

    @Test
    void processJobMarksDoneNotifiesCallbackAndCleansTempFile() throws Exception {
        var tempFile = Files.createTempFile("profile-photo-test", ".png");
        Files.writeString(tempFile, "image-content");

        var jobRepository = new InMemoryJobRepository();
        var persistenceRepository = new NoOpPersistenceRepository();
        var storageRepository = new FakeStorageRepository();
        var stableDiffusionService = new FakeStableDiffusionService();
        var webhookNotifier = new RecordingWebhookNotifier();
        var events = new ProfilePhotoJobEvents();

        var job = jobRepository.create("customer-123",
                                       new ProfilePhoto(UUID.randomUUID().toString(),
                                                        tempFile.toAbsolutePath().toString(),
                                                        null),
                                       "http://callback.test/hook");

        var processor = new ProfilePhotoAsyncProcessor(jobRepository,
                                                       persistenceRepository,
                                                       storageRepository,
                                                       stableDiffusionService,
                                                       events,
                                                       webhookNotifier);

        processor.process(job.id());

        var updated = jobRepository.findById(job.id()).orElseThrow();
        assertEquals(ProcessingStatus.DONE, updated.status());
        assertTrue(updated.generatedPhotoUrl().contains("generated"));
        assertFalse(Files.exists(tempFile), "temp file should be cleaned up");
        assertEquals("http://callback.test/hook", webhookNotifier.lastCallbackUrl.get());
        assertEquals(ProcessingStatus.DONE, webhookNotifier.lastStatus.get().status());
    }

    private static class InMemoryJobRepository implements ProfilePhotoJobRepository {
        private final Map<String, MutableJob> jobs = new ConcurrentHashMap<>();

        @Override
        public ProfilePhotoJob create(String customerId, ProfilePhoto profilePhoto, String callbackUrl) {
            var id = UUID.randomUUID().toString();
            var now = Instant.now();
            jobs.put(id, new MutableJob(id, customerId, profilePhoto, ProcessingStatus.PENDING, null, null, callbackUrl, null, now, now));
            return toDomain(jobs.get(id));
        }

        @Override
        public Optional<ProfilePhotoJob> findById(String jobId) {
            return Optional.ofNullable(jobs.get(jobId)).map(this::toDomain);
        }

        @Override
        public void updateStatus(String jobId, ProcessingStatus status, String error) {
            jobs.computeIfPresent(jobId, (id, job) -> job.withStatus(status, error));
        }

        @Override
        public void updateResult(String jobId, String originalPhotoUrl, String generatedPhotoUrl) {
            jobs.computeIfPresent(jobId, (id, job) -> job.withResult(originalPhotoUrl, generatedPhotoUrl));
        }

        private ProfilePhotoJob toDomain(MutableJob job) {
            return new ProfilePhotoJob(job.id,
                                       job.customerId,
                                       job.profilePhoto,
                                       job.status,
                                       job.originalPhotoUrl,
                                       job.generatedPhotoUrl,
                                       job.callbackUrl,
                                       job.error,
                                       job.createdAt,
                                       job.updatedAt);
        }

        private record MutableJob(String id,
                                  String customerId,
                                  ProfilePhoto profilePhoto,
                                  ProcessingStatus status,
                                  String originalPhotoUrl,
                                  String generatedPhotoUrl,
                                  String callbackUrl,
                                  String error,
                                  Instant createdAt,
                                  Instant updatedAt) {
            MutableJob withStatus(ProcessingStatus status, String error) {
                return new MutableJob(id, customerId, profilePhoto, status, originalPhotoUrl, generatedPhotoUrl, callbackUrl, error, createdAt, Instant.now());
            }

            MutableJob withResult(String originalPhotoUrl, String generatedPhotoUrl) {
                return new MutableJob(id, customerId, profilePhoto, status, originalPhotoUrl, generatedPhotoUrl, callbackUrl, error, createdAt, Instant.now());
            }
        }
    }

    private static class NoOpPersistenceRepository implements ProfilePhotoPersistenceRepository {
        @Override
        public void save(String customerId, ProfilePhoto profilePhoto) {
            // no-op
        }
    }

    private static class FakeStorageRepository implements ProfilePhotoStorageRepository {
        @Override
        public Uni<String> store(String customerId, ProfilePhoto profilePhoto) {
            return Uni.createFrom().item("https://s3.example/" + customerId + "/" + profilePhoto.id());
        }

        @Override
        public Uni<String> store(String customerId, ProfilePhoto profilePhoto, String base64) {
            return Uni.createFrom().item("https://s3.example/" + customerId + "/" + profilePhoto.id() + "/generated");
        }
    }

    private static class FakeStableDiffusionService extends StableDiffusionService {
        FakeStableDiffusionService() {
            super(null);
        }

        @Override
        public Uni<String> generate(ProfilePhoto profilePhoto) {
            return Uni.createFrom().item("YmFzZTY0LWltYWdl");
        }
    }

    private static class RecordingWebhookNotifier extends WebhookNotifier {
        private final AtomicReference<String> lastCallbackUrl = new AtomicReference<>();
        private final AtomicReference<ProfilePhotoJobStatus> lastStatus = new AtomicReference<>();

        RecordingWebhookNotifier() {
            super(HttpClient.newHttpClient(), new ObjectMapper());
        }

        @Override
        public void notify(String callbackUrl, ProfilePhotoJobStatus status) {
            lastCallbackUrl.set(callbackUrl);
            lastStatus.set(status);
        }
    }
}
