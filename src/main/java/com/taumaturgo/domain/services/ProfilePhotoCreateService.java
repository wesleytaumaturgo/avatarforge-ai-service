package com.taumaturgo.domain.services;

import com.taumaturgo.domain.models.ProfilePhoto;
import com.taumaturgo.domain.repositories.ProfilePhotoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class ProfilePhotoCreateService {
    private final ProfilePhotoRepository repository;

    public ProfilePhotoCreateService(ProfilePhotoRepository repository) {
        this.repository = repository;
    }

    public void save(String customerId, ProfilePhoto profilePhoto) {
        repository.registerEntities(Map.of(customerId, profilePhoto));
        repository.commit();
    }
}
