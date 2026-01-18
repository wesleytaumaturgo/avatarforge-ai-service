package com.taumaturgo.domain.repositories;

import com.taumaturgo.domain.models.ProfilePhoto;

public interface ProfilePhotoPersistenceRepository {
    void save(String customerId, ProfilePhoto profilePhoto);
}
