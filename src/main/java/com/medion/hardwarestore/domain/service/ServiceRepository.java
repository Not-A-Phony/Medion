package com.medion.hardwarestore.domain.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID>, JpaSpecificationExecutor<ServiceEntity> {
}
