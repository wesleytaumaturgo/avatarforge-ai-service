package com.taumaturgo.application.dto;

import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public record ProfilePhoto(FileUpload fileUpload) {
    public static ProfilePhoto create(FileUpload fileUpload) {
        return new ProfilePhoto(fileUpload);
    }

    public com.taumaturgo.domain.models.ProfilePhoto toDomain(String customerId) {
        try {
            Path tempFile = Files.createTempFile("profile-photo-" + customerId + "-", "-" + fileUpload.fileName());
            Files.copy(fileUpload.uploadedFile(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return new com.taumaturgo.domain.models.ProfilePhoto(UUID.randomUUID().toString(),
                                                                 tempFile.toAbsolutePath().toString(),
                                                                 null);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to persist upload", exception);
        }
    }
}
