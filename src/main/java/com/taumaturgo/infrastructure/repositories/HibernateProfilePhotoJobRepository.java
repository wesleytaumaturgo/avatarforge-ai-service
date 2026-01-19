package com.taumaturgo.infrastructure.repositories;

import com.taumaturgo.domain.models.ProfilePhoto;
import com.taumaturgo.domain.models.ProfilePhotoJob;
import com.taumaturgo.domain.models.ProcessingStatus;
import com.taumaturgo.domain.repositories.ProfilePhotoJobRepository;
import com.taumaturgo.infrastructure.repositories.entities.ProfilePhotoJobEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class HibernateProfilePhotoJobRepository implements ProfilePhotoJobRepository {
    private final EntityManager entityManager;

    public HibernateProfilePhotoJobRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public ProfilePhotoJob create(String customerId, ProfilePhoto profilePhoto, String callbackUrl) {
        var entity = ProfilePhotoJobEntity.create(customerId, profilePhoto, callbackUrl);
        entityManager.persist(entity);
        entityManager.flush();
        return entity.toDomain();
    }

    @Override
    public Optional<ProfilePhotoJob> findById(String jobId) {
        return Optional.ofNullable(entityManager.find(ProfilePhotoJobEntity.class, jobId))
                       .map(ProfilePhotoJobEntity::toDomain);
    }

    @Override
    @Transactional
    public void updateStatus(String jobId, ProcessingStatus status, String error) {
        var entity = entityManager.find(ProfilePhotoJobEntity.class, jobId);
        if (entity == null) {
            return;
        }
        entity.status = status;
        entity.error = error;
        entity.updatedAt = Instant.now();
        entityManager.merge(entity);
    }

    @Override
    @Transactional
    public void updateResult(String jobId, String originalPhotoUrl, String generatedPhotoUrl) {
        var entity = entityManager.find(ProfilePhotoJobEntity.class, jobId);
        if (entity == null) {
            return;
        }
        entity.originalPhotoPath = originalPhotoUrl;
        entity.generatedPhotoUrl = generatedPhotoUrl;
        entity.updatedAt = Instant.now();
        entityManager.merge(entity);
    }
}
