package com.taumaturgo.application.dto;

import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.UUID;

public record ProfilePhoto(FileUpload fileUpload) {
    public static ProfilePhoto create(FileUpload fileUpload) {
        return new ProfilePhoto(fileUpload);
    }

    public com.taumaturgo.domain.models.ProfilePhoto toDomain() {
        return new com.taumaturgo.domain.models.ProfilePhoto(UUID.randomUUID().toString(),
                                                             fileUpload().uploadedFile().toAbsolutePath().toString(),
                                                             null);
    }
}
