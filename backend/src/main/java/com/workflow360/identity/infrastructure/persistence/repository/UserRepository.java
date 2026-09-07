package com.workflow360.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow360.identity.domain.UserStatus;
import com.workflow360.identity.infrastructure.persistence.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID>{
	
	Optional<UserEntity> findByEmailIgnoreCase(String email);
	
	boolean existsByEmailIgnoreCase(String email);
	
	List<UserEntity> findAllByOrderByCreatedAtDesc();

	
	@Query("""
	        SELECT user
	        FROM UserEntity user
	        WHERE (
	            :query IS NULL
	            OR LOWER(user.email) LIKE LOWER(CONCAT('%', :query, '%'))
	            OR LOWER(user.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
	        )
	        AND (
	            :status IS NULL
	            OR user.status = :status
	        )
	        """)
	Page<UserEntity> search(@Param("query") String query , @Param("status") UserStatus status,Pageable page);
}
