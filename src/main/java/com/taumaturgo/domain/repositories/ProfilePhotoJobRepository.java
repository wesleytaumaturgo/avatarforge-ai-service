package com.taumaturgo.domain.repositories;

import com.taumaturgo.domain.models.ProfilePhoto;
import com.taumaturgo.domain.models.ProfilePhotoJob;
import com.taumaturgo.domain.models.ProcessingStatus;

import java.util.Optional;

public interface ProfilePhotoJobRepository {
    ProfilePhotoJob create(String customerId, ProfilePhoto profilePhoto, String callbackUrl);

    Optional<ProfilePhotoJob> findById(String jobId);

    void updateStatus(String jobId, ProcessingStatus status, String error);

    void updateResult(String jobId, String originalPhotoUrl, String generatedPhotoUrl);
}
