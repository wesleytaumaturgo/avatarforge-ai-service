package com.taumaturgo.application;

import com.taumaturgo.application.dto.ProfilePhotoJobStatus;
import com.taumaturgo.domain.repositories.ProfilePhotoJobRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

@ApplicationScoped
public class ProfilePhotoJobStatusService {
    private final ProfilePhotoJobRepository jobRepository;

    public ProfilePhotoJobStatusService(ProfilePhotoJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public ProfilePhotoJobStatus find(String jobId) {
        return ProfilePhotoJobStatus.fromDomain(jobRepository.findById(jobId)
                                                             .orElseThrow(NoSuchElementException::new));
    }

    public ProfilePhotoJobStatus waitForCompletion(String jobId, Duration wait) {
        var deadline = Instant.now().plus(wait);
        var status = find(jobId);

        while (!status.status().isTerminal() && Instant.now().isBefore(deadline)) {
            sleepBriefly();
            status = find(jobId);
        }

        return status;
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
