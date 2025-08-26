package io.github.bookrentalteam.bookrental.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import io.github.bookrentalteam.bookrental.common.exception.BusinessException;
import io.github.bookrentalteam.bookrental.common.exception.ValidationException;

public class Rental {
	private static long sequence = 0;

	private Long id;
	private Long bookId;
	private Long memberId;
	private LocalDate rentedAt;
	private LocalDate dueAt;
	private LocalDate returnedAt;
	private RentalStatus status;

	public Rental(Long bookId, Long memberId) {
		if (bookId == null || bookId <= 0) {
			throw new ValidationException("bookId는 필수입니다.");
		}
		if (memberId == null || memberId <= 0) {
			throw new ValidationException("memberId는 필수입니다.");
		}

		this.id = ++sequence;
		this.bookId = bookId;
		this.memberId = memberId;
		this.rentedAt = LocalDate.now();
		this.dueAt = rentedAt.plusDays(14);
		this.status = RentalStatus.RENTED;
	}

	// getter
	public Long getId() {
		return id;
	}

	public Long getBookId() {
		return bookId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public LocalDate getRentedAt() {
		return rentedAt;
	}

	public LocalDate getDueAt() {
		return dueAt;
	}

	public LocalDate getReturnedAt() {
		return returnedAt;
	}

	public RentalStatus getStatus() {
		return status;
	}

	// 반납 처리
	public void markReturned(LocalDate date) {
		if (status == RentalStatus.RETURNED) {
			throw new BusinessException("이미 반납된 대여입니다.");
		}
		this.returnedAt = (date != null) ? date : LocalDate.now();
		this.status = RentalStatus.RETURNED;
	}

	// 연체 여부
	public boolean isOverdue() {
		return status == RentalStatus.RENTED && dueAt.isBefore(LocalDate.now());
	}

	// 연체 일수
	public long overdueDays() {
		return isOverdue() ? ChronoUnit.DAYS.between(dueAt, LocalDate.now()) : 0;
	}
}
