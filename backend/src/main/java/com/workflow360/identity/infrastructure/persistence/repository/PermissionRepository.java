package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow360.identity.infrastructure.persistence.entity.PermissionEntity;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {
	
	Optional<PermissionEntity> findByCode(String code);

}
