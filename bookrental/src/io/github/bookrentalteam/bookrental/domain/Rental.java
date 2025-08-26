package io.github.bookrentalteam.bookrental.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import io.github.bookrentalteam.bookrental.common.exception.BusinessException;
import io.github.bookrentalteam.bookrental.common.exception.ValidationException;

/**
 * 대여 엔티티 (불변, auto-increment ID)
 */
public record Rental(long id, long bookId, long memberId, LocalDate rentedAt, LocalDate dueAt, LocalDate returnedAt,
		RentalStatus status) {
	private static long sequence = 0;

	public Rental {
		if (bookId <= 0) {
			throw new ValidationException("bookId는 필수입니다.");
		}
		if (memberId <= 0) {
			throw new ValidationException("memberId는 필수입니다.");
		}
		if (rentedAt == null) {
			rentedAt = LocalDate.now();
		}
		if (dueAt == null) {
			dueAt = rentedAt.plusDays(14);
		}
		if (status == null) {
			status = RentalStatus.RENTED;
		}
	}

	// 자동 ID 발급 생성자
	public Rental(long bookId, long memberId) {
		this(++sequence, bookId, memberId, LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.RENTED);
	}

	public Rental markReturned(LocalDate date) {
		if (status == RentalStatus.RETURNED) {
			throw new BusinessException("이미 반납된 대여입니다.");
		}
		if (date == null) {
			date = LocalDate.now();
		}
		return new Rental(id, bookId, memberId, rentedAt, dueAt, date, RentalStatus.RETURNED);
	}

	/** 연체 여부 */
	public boolean isOverdue() {
		return status == RentalStatus.RENTED && dueAt.isBefore(LocalDate.now());
	}

	/** 연체 일수 */
	public long overdueDays() {
		return isOverdue() ? ChronoUnit.DAYS.between(dueAt, LocalDate.now()) : 0;
	}
}
