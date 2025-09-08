package io.github.bookrentalteam.bookrental.domain;

import java.time.LocalDateTime;

public class Rental {
	private Long id; // rental_id (PK, AUTO_INCREMENT)
	private Long memberId; // FK -> member.member_id
	private LocalDateTime rentedAt; // DATETIME
	private RentalStatus rentalStatus; // OPEN / CLOSED
	private LocalDateTime createdAt; // DATETIME
	private LocalDateTime updatedAt; // DATETIME

	public Rental() {
	}

	public Rental(Long memberId) {
		if (memberId == null || memberId <= 0) {
			throw new IllegalArgumentException("memberId는 필수입니다.");
		}
		this.memberId = memberId;
		this.rentedAt = LocalDateTime.now();
		this.rentalStatus = RentalStatus.OPEN;
	}

	// 상태 보조
	public void setOpen() {
		this.rentalStatus = RentalStatus.OPEN;
	}

	public void setClosed() {
		this.rentalStatus = RentalStatus.CLOSED;
	}

	// ===== getter/setter =====
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMemberId() {
		return memberId;
	}

	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	public LocalDateTime getRentedAt() {
		return rentedAt;
	}

	public void setRentedAt(LocalDateTime rentedAt) {
		this.rentedAt = rentedAt;
	}

	public RentalStatus getRentalStatus() {
		return rentalStatus;
	}

	public void setRentalStatus(RentalStatus rentalStatus) {
		this.rentalStatus = rentalStatus;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
