package com.taumaturgo.infrastructure.repositories.entities;

import com.taumaturgo.domain.models.ProfilePhoto;
import com.taumaturgo.domain.models.ProfilePhotoJob;
import com.taumaturgo.domain.models.ProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_photo_jobs")
public class ProfilePhotoJobEntity {
    @Id
    public String id;

    @Column(name = "customer_id")
    public String customerId;

    @Column(name = "profile_photo_id")
    public String profilePhotoId;

    @Column(name = "original_photo_path")
    public String originalPhotoPath;

    @Column(name = "generated_photo_url")
    public String generatedPhotoUrl;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    public ProcessingStatus status;

    @Column(name = "error")
    public String error;

    @Column(name = "callback_url")
    public String callbackUrl;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    public static ProfilePhotoJobEntity create(String customerId, ProfilePhoto profilePhoto, String callbackUrl) {
        var entity = new ProfilePhotoJobEntity();
        entity.id = UUID.randomUUID().toString();
        entity.customerId = customerId;
        entity.profilePhotoId = profilePhoto.id();
        entity.originalPhotoPath = profilePhoto.originalPhoto();
        entity.callbackUrl = callbackUrl;
        entity.status = ProcessingStatus.PENDING;
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }

    public ProfilePhotoJob toDomain() {
        return new ProfilePhotoJob(id,
                                   customerId,
                                   new ProfilePhoto(profilePhotoId, originalPhotoPath, generatedPhotoUrl),
                                   status,
                                   originalPhotoPath,
                                   generatedPhotoUrl,
                                   callbackUrl,
                                   error,
                                   createdAt,
                                   updatedAt);
    }
}
