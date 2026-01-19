package com.taumaturgo.application;

import com.taumaturgo.application.dto.Customer;
import com.taumaturgo.application.dto.ProfilePhoto;
import com.taumaturgo.application.dto.ProfilePhotoJobStatus;
import com.taumaturgo.domain.services.CustomerReadService;
import com.taumaturgo.domain.services.ProfilePhotoCreateService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ApplicationService {
    private final CustomerReadService customerReadService;
    private final ProfilePhotoCreateService profilePhotoCreateService;
    private final ProfilePhotoJobStatusService jobStatusService;

    public ApplicationService(CustomerReadService customerReadService,
                              ProfilePhotoCreateService profilePhotoCreateService,
                              ProfilePhotoJobStatusService jobStatusService) {
        this.customerReadService = customerReadService;
        this.profilePhotoCreateService = profilePhotoCreateService;
        this.jobStatusService = jobStatusService;
    }

    public List<Customer> searchCustomers() {
        return customerReadService.find().stream().map(Customer::fromDomain).toList();
    }

    public Customer getCustomer(String customerId) {
        return Customer.fromDomain(customerReadService.findById(customerId));
    }

    public ProfilePhotoJobStatus persistProfilePhoto(String customerId, ProfilePhoto dto, String callbackUrl) {
        return profilePhotoCreateService.submit(customerId, dto.toDomain(customerId), callbackUrl);
    }

    public ProfilePhotoJobStatus findJobStatus(String jobId) {
        return jobStatusService.find(jobId);
    }

    public ProfilePhotoJobStatus waitForJobStatus(String jobId, long waitSeconds) {
        return jobStatusService.waitForCompletion(jobId, java.time.Duration.ofSeconds(waitSeconds));
    }
}
