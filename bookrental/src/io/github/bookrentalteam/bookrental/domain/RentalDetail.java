package io.github.bookrentalteam.bookrental.domain;

import java.time.LocalDate;
import java.util.Objects;

public class RentalDetail {
	private Long rentalId; // PK part 1
	private Long bookId; // PK part 2
	private LocalDate dueAt; // 반납 예정일
	private LocalDate returnedAt; // 반납일(Nullable)
	private DetailStatus detailStatus; // RENTED/OVERDUE/RETURNED/LOST
	private int extensionCount; // 연장 횟수 (0+)

	// 생성 시 기본값: 대여중(RENTED), dueAt=오늘+14일
	public static RentalDetail of(Long rentalId, Long bookId) {
		if (rentalId == null || rentalId <= 0) {
			throw new IllegalArgumentException("rentalId는 필수입니다.");
		}
		if (bookId == null || bookId <= 0) {
			throw new IllegalArgumentException("bookId는 필수입니다.");
		}
		RentalDetail d = new RentalDetail();
		d.rentalId = rentalId;
		d.bookId = bookId;
		d.dueAt = LocalDate.now().plusDays(14);
		d.detailStatus = DetailStatus.RENTED;
		d.extensionCount = 0;
		return d;
	}

	// ---- 도메인 행위 ----
	public void markReturned(LocalDate date) {
		if (detailStatus != DetailStatus.RENTED && detailStatus != DetailStatus.OVERDUE) {
			throw new IllegalStateException("반납 가능한 상태가 아닙니다.");
		}
		this.returnedAt = (date != null) ? date : LocalDate.now();
		this.detailStatus = DetailStatus.RETURNED;
	}

	public void extend7Days() {
		if (detailStatus != DetailStatus.RENTED) {
			throw new IllegalStateException("대여중인 항목만 연장할 수 있습니다.");
		}
		if (dueAt.isBefore(LocalDate.now())) {
			throw new IllegalStateException("연체 상태는 연장할 수 없습니다.");
		}
		if (extensionCount >= 1) {
			throw new IllegalStateException("연장은 1회만 가능합니다.");
		}
		this.dueAt = this.dueAt.plusDays(7);
		this.extensionCount += 1;
	}

	public void refreshOverdue() {
		if (detailStatus == DetailStatus.RENTED && dueAt.isBefore(LocalDate.now())) {
			this.detailStatus = DetailStatus.OVERDUE;
		}
	}

	public void markLost() {
		if (detailStatus == DetailStatus.RETURNED) {
			throw new IllegalStateException("이미 반납된 항목은 분실 처리할 수 없습니다.");
		}
		this.detailStatus = DetailStatus.LOST;
	}

	// ---- equals/hashCode: 복합키 기준 ----
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RentalDetail)) {
			return false;
		}
		RentalDetail that = (RentalDetail) o;
		return Objects.equals(rentalId, that.rentalId) && Objects.equals(bookId, that.bookId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(rentalId, bookId);
	}

	// ===== getter/setter =====
	public Long getRentalId() {
		return rentalId;
	}

	public void setRentalId(Long rentalId) {
		this.rentalId = rentalId;
	}

	public Long getBookId() {
		return bookId;
	}

	public void setBookId(Long bookId) {
		this.bookId = bookId;
	}

	public LocalDate getDueAt() {
		return dueAt;
	}

	public void setDueAt(LocalDate dueAt) {
		this.dueAt = dueAt;
	}

	public LocalDate getReturnedAt() {
		return returnedAt;
	}

	public void setReturnedAt(LocalDate returnedAt) {
		this.returnedAt = returnedAt;
	}

	public DetailStatus getDetailStatus() {
		return detailStatus;
	}

	public void setDetailStatus(DetailStatus detailStatus) {
		this.detailStatus = detailStatus;
	}

	public int getExtensionCount() {
		return extensionCount;
	}

	public void setExtensionCount(int extensionCount) {
		this.extensionCount = extensionCount;
	}
}
