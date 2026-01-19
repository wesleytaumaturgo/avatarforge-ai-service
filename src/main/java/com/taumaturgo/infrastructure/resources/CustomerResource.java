package com.taumaturgo.infrastructure.resources;

import com.taumaturgo.application.ApplicationService;
import com.taumaturgo.application.dto.Customer;
import com.taumaturgo.application.dto.ProfilePhoto;
import com.taumaturgo.application.dto.ProfilePhotoJobStatus;
import com.taumaturgo.application.events.ProfilePhotoJobEvents;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestSseElementType;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.List;
import java.util.NoSuchElementException;

@Path("customers")
public class CustomerResource {
    private final ApplicationService service;
    private final ProfilePhotoJobEvents jobEvents;

    public CustomerResource(ApplicationService service, ProfilePhotoJobEvents jobEvents) {
        this.service = service;
        this.jobEvents = jobEvents;
    }

    @GET
    public List<Customer> searchCustomers() {
        return service.searchCustomers();
    }

    @GET
    @Path("/{id}")
    public Customer getCustomer(@PathParam("id") String id) {
        try {
            return service.getCustomer(id);
        } catch (NoSuchElementException exception) {
            throw new NotFoundException();
        }
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ResponseStatus(RestResponse.StatusCode.ACCEPTED)
    public RestResponse<ProfilePhotoJobStatus> persistProfilePhoto(@PathParam("id") String id,
                                                                   @RestForm("photo") FileUpload fileUpload,
                                                                   @RestForm("callbackUrl") String callbackUrl) {
        var status = service.persistProfilePhoto(id, ProfilePhoto.create(fileUpload), callbackUrl);
        return RestResponse.ResponseBuilder.create(RestResponse.Status.ACCEPTED, status)
                                           .header("Location", "/customers/%s/photos/%s/status".formatted(id, status.jobId()))
                                           .build();
    }

    @GET
    @Path("/{id}/photos/{jobId}/status")
    public ProfilePhotoJobStatus getJobStatus(@PathParam("id") String id,
                                              @PathParam("jobId") String jobId,
                                              @QueryParam("waitSeconds") @DefaultValue("0") long waitSeconds) {
        try {
            var status = waitSeconds > 0
                    ? service.waitForJobStatus(jobId, waitSeconds)
                    : service.findJobStatus(jobId);

            if (!status.customerId().equals(id)) {
                throw new NotFoundException();
            }
            return status;
        } catch (NoSuchElementException exception) {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("/{id}/photos/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestSseElementType(MediaType.APPLICATION_JSON)
    public Multi<ProfilePhotoJobStatus> streamJobStatus(@PathParam("id") String id) {
        return jobEvents.streamByCustomer(id);
    }
}
