package com.taumaturgo.application.events;

import com.taumaturgo.application.dto.ProfilePhotoJobStatus;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProfilePhotoJobEvents {
    private final BroadcastProcessor<ProfilePhotoJobStatus> processor = BroadcastProcessor.create();

    public void publish(ProfilePhotoJobStatus status) {
        processor.onNext(status);
    }

    public Multi<ProfilePhotoJobStatus> streamByCustomer(String customerId) {
        return processor.filter(status -> status.customerId().equals(customerId));
    }
}
