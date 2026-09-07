package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow360.identity.infrastructure.persistence.entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID>{
	
	Optional<RoleEntity> findByCode(String code);

}
