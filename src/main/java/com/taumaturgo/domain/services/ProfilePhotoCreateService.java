package com.taumaturgo.domain.services;

import com.taumaturgo.application.dto.ProfilePhotoJobStatus;
import com.taumaturgo.application.events.ProfilePhotoJobEvents;
import com.taumaturgo.domain.models.ProfilePhoto;
import com.taumaturgo.domain.models.ProfilePhotoJob;
import com.taumaturgo.domain.repositories.ProfilePhotoJobRepository;
import com.taumaturgo.infrastructure.async.ProfilePhotoAsyncProcessor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProfilePhotoCreateService {
    private final ProfilePhotoJobRepository jobRepository;
    private final ProfilePhotoAsyncProcessor asyncProcessor;
    private final ProfilePhotoJobEvents events;

    public ProfilePhotoCreateService(ProfilePhotoJobRepository jobRepository,
                                     ProfilePhotoAsyncProcessor asyncProcessor,
                                     ProfilePhotoJobEvents events) {
        this.jobRepository = jobRepository;
        this.asyncProcessor = asyncProcessor;
        this.events = events;
    }

    public ProfilePhotoJobStatus submit(String customerId, ProfilePhoto profilePhoto, String callbackUrl) {
        ProfilePhotoJob job = jobRepository.create(customerId, profilePhoto, callbackUrl);
        events.publish(ProfilePhotoJobStatus.fromDomain(job));
        asyncProcessor.enqueue(job.id());
        return ProfilePhotoJobStatus.fromDomain(job);
    }
}
