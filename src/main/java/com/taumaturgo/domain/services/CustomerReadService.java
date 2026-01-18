package com.taumaturgo.domain.services;

import com.taumaturgo.domain.models.Customer;
import com.taumaturgo.domain.repositories.CustomerRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class CustomerReadService {
    private final CustomerRepository repository;

    public CustomerReadService(CustomerRepository repository) {
        this.repository = repository;
    }

    public List<Customer> find() {
        return repository.findAll();
    }

    public Customer findById(String id) {
        return repository.findById(id).orElseThrow(NoSuchElementException::new);
    }
}
