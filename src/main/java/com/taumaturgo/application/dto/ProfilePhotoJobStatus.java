package com.taumaturgo.application.dto;

import com.taumaturgo.domain.models.ProfilePhotoJob;
import com.taumaturgo.domain.models.ProcessingStatus;

import java.time.Instant;

public record ProfilePhotoJobStatus(String jobId,
                                    String customerId,
                                    ProcessingStatus status,
                                    String originalPhotoUrl,
                                    String generatedPhotoUrl,
                                    String error,
                                    Instant createdAt,
                                    Instant updatedAt) {
    public static ProfilePhotoJobStatus fromDomain(ProfilePhotoJob job) {
        return new ProfilePhotoJobStatus(job.id(),
                                         job.customerId(),
                                         job.status(),
                                         job.originalPhotoUrl(),
                                         job.generatedPhotoUrl(),
                                         job.error(),
                                         job.createdAt(),
                                         job.updatedAt());
    }
}
