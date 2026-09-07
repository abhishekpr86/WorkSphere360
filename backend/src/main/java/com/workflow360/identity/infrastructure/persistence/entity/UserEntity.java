package com.workflow360.identity.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.workflow360.identity.domain.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")

public class UserEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "display_name", nullable = false, length = 150)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private long version;

	public UserEntity() {

	}

	public UserEntity(UUID id, String email, String displayName, UserStatus status, boolean emailVerified) {

		this.id = id;
		this.email = email;
		this.displayName = displayName;
		this.status = status;
		this.emailVerified = emailVerified;

	}

	
	
	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public UserEntity(UUID id, String email, String displayName, UserStatus status) {
		super();
		this.id = id;
		this.email = email;
		this.displayName = displayName;
		this.status = status;
	}
	
	public void updateDisplayName(String displayName) {
		this.displayName=displayName;
	}
	
	public void changeStatus(UserStatus newStatus) {
		if(this.status == newStatus) {
			return;
		}
		
		if(this.status == UserStatus.DISABLED && newStatus != UserStatus.DISABLED) {
			throw new IllegalStateException("A dsiable user cannot be Activared Again");
		}
		this.status = newStatus;
	}

	
	
}
