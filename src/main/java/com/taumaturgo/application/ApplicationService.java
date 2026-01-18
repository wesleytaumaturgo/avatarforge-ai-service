package com.taumaturgo.application;

import com.taumaturgo.application.dto.Customer;
import com.taumaturgo.application.dto.ProfilePhoto;
import com.taumaturgo.domain.services.CustomerReadService;
import com.taumaturgo.domain.services.ProfilePhotoCreateService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ApplicationService {
    private final CustomerReadService customerReadService;
    private final ProfilePhotoCreateService profilePhotoCreateService;

    public ApplicationService(CustomerReadService customerReadService,
                              ProfilePhotoCreateService profilePhotoCreateService) {
        this.customerReadService = customerReadService;
        this.profilePhotoCreateService = profilePhotoCreateService;
    }

    public List<Customer> searchCustomers() {
        return customerReadService.find().stream().map(Customer::fromDomain).toList();
    }

    public Customer getCustomer(String customerId) {
        return Customer.fromDomain(customerReadService.findById(customerId));
    }

    public void persistProfilePhoto(String customerId, ProfilePhoto dto) {
        profilePhotoCreateService.save(customerId, dto.toDomain());
    }
}
