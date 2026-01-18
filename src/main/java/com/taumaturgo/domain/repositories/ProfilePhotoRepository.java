package com.taumaturgo.domain.repositories;

import com.taumaturgo.domain.models.ProfilePhoto;

import java.util.Map;

public interface ProfilePhotoRepository {
    void registerEntities(Map<String, ProfilePhoto> entities);
    void commit();
    void rollback();
}
