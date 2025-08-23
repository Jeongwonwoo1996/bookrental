package io.github.bookrentalteam.bookrental;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 대여 엔티티 (불변, auto-increment ID)
 */
public record Rental(
        long id,
        long bookId,
        long memberId,
        LocalDate rentedAt,
        LocalDate dueAt,
        LocalDate returnedAt,
        RentalStatus status
) {
    private static long sequence = 0;

    public Rental {
        if (bookId <= 0) throw new IllegalArgumentException("bookId 필수");
        if (memberId <= 0) throw new IllegalArgumentException("memberId 필수");
        if (rentedAt == null) rentedAt = LocalDate.now();
        if (dueAt == null) dueAt = rentedAt.plusDays(14);
        if (status == null) status = RentalStatus.RENTED;
    }

    public Rental(long bookId, long memberId) {
        this(++sequence, bookId, memberId, LocalDate.now(), LocalDate.now().plusDays(14), null, RentalStatus.RENTED);
    }

    public Rental markReturned(LocalDate date) {
        if (status == RentalStatus.RETURNED) throw new IllegalStateException("이미 반납됨");
        if (date == null) date = LocalDate.now();
        return new Rental(id, bookId, memberId, rentedAt, dueAt, date, RentalStatus.RETURNED);
    }

    public boolean isOverdue() {
        return status == RentalStatus.RENTED && dueAt.isBefore(LocalDate.now());
    }

    public long overdueDays() {
        return isOverdue() ? ChronoUnit.DAYS.between(dueAt, LocalDate.now()) : 0;
    }
}
