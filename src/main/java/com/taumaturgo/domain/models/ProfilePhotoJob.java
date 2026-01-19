package com.taumaturgo.domain.models;

import java.time.Instant;

public record ProfilePhotoJob(String id,
                              String customerId,
                              ProfilePhoto profilePhoto,
                              ProcessingStatus status,
                              String originalPhotoUrl,
                              String generatedPhotoUrl,
                              String callbackUrl,
                              String error,
                              Instant createdAt,
                               Instant updatedAt) {
}
