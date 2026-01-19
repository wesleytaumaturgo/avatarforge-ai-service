package com.taumaturgo.infrastructure.grpc;

import com.google.protobuf.Timestamp;
import com.taumaturgo.application.ProfilePhotoJobStatusService;
import com.taumaturgo.application.dto.ProfilePhotoJobStatus;
import com.taumaturgo.application.events.ProfilePhotoJobEvents;
import com.taumaturgo.domain.models.ProcessingStatus;
import com.taumaturgo.grpc.JobStatusRequest;
import com.taumaturgo.grpc.JobStatusResponse;
import com.taumaturgo.grpc.MutinyProfilePhotoJobGrpcGrpc;
import com.taumaturgo.grpc.Status;
import com.taumaturgo.grpc.StatusStreamRequest;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.NoSuchElementException;

@Singleton
@GrpcService
public class ProfilePhotoJobGrpcService extends MutinyProfilePhotoJobGrpcGrpc.ProfilePhotoJobGrpcImplBase {
    private final ProfilePhotoJobStatusService statusService;
    private final ProfilePhotoJobEvents jobEvents;

    public ProfilePhotoJobGrpcService(ProfilePhotoJobStatusService statusService, ProfilePhotoJobEvents jobEvents) {
        this.statusService = statusService;
        this.jobEvents = jobEvents;
    }

    @Override
    public Uni<JobStatusResponse> getStatus(JobStatusRequest request) {
        var waitSeconds = Math.max(0, request.getWaitSeconds());
        return Uni.createFrom()
                  .item(() -> waitSeconds > 0
                          ? statusService.waitForCompletion(request.getJobId(), Duration.ofSeconds(waitSeconds))
                          : statusService.find(request.getJobId()))
                  .onItem()
                  .transform(this::toProto)
                  .onFailure(NoSuchElementException.class)
                  .transform(throwable -> notFound("Job %s not found".formatted(request.getJobId())));
    }

    @Override
    public Multi<JobStatusResponse> streamStatus(StatusStreamRequest request) {
        if (request.getCustomerId().isBlank()) {
            return Multi.createFrom().failure(invalidArgument("customer_id is required"));
        }
        var jobId = request.getJobId();

        Multi<ProfilePhotoJobStatus> updates = jobEvents.streamByCustomer(request.getCustomerId());
        if (!jobId.isBlank()) {
            updates = updates.filter(status -> status.jobId().equals(jobId));
        }

        Multi<ProfilePhotoJobStatus> initial = Multi.createFrom().empty();
        if (!jobId.isBlank()) {
            initial = Multi.createFrom().item(() -> {
                        var current = statusService.find(jobId);
                        if (!current.customerId().equals(request.getCustomerId())) {
                            throw new NoSuchElementException();
                        }
                        return current;
                    })
                    .onFailure(NoSuchElementException.class)
                    .transform(throwable -> notFound("Job %s not found".formatted(jobId)));
        }

        return Multi.createBy().concatenating().streams(initial, updates)
                      .onItem()
                      .transform(this::toProto);
    }

    private JobStatusResponse toProto(ProfilePhotoJobStatus status) {
        return JobStatusResponse.newBuilder()
                                .setJobId(status.jobId())
                                .setCustomerId(status.customerId())
                                .setStatus(toProtoStatus(status.status()))
                                .setOriginalPhotoUrl(nullToEmpty(status.originalPhotoUrl()))
                                .setGeneratedPhotoUrl(nullToEmpty(status.generatedPhotoUrl()))
                                .setError(nullToEmpty(status.error()))
                                .setCreatedAt(toTimestamp(status.createdAt()))
                                .setUpdatedAt(toTimestamp(status.updatedAt()))
                                .build();
    }

    private Status toProtoStatus(ProcessingStatus status) {
        return switch (status) {
            case PENDING -> Status.STATUS_PENDING;
            case PROCESSING -> Status.STATUS_PROCESSING;
            case DONE -> Status.STATUS_DONE;
            case FAILED -> Status.STATUS_FAILED;
        };
    }

    private Timestamp toTimestamp(java.time.Instant instant) {
        return Timestamp.newBuilder()
                        .setSeconds(instant.getEpochSecond())
                        .setNanos(instant.getNano())
                        .build();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private StatusRuntimeException notFound(String message) {
        return io.grpc.Status.NOT_FOUND.withDescription(message).asRuntimeException();
    }

    private StatusRuntimeException invalidArgument(String message) {
        return io.grpc.Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException();
    }
}
