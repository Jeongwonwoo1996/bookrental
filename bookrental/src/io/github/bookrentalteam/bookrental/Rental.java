package io.github.bookrentalteam.bookrental;

import java.time.LocalDate;
import java.util.UUID;

public record Rental(UUID id, UUID bookId, UUID memberId,
                     LocalDate rentedAt, LocalDate dueAt, LocalDate returnedAt,
                     RentalStatus status) {

    public Rental {
        if (id == null) id = UUID.randomUUID();
        if (bookId == null) throw new IllegalArgumentException("bookId 필수");
        if (memberId == null) throw new IllegalArgumentException("memberId 필수");
        if (rentedAt == null) rentedAt = LocalDate.now();
        if (dueAt == null) dueAt = rentedAt.plusDays(14);
        if (status == null) status = RentalStatus.RENTED;
    }

    public Rental markReturned(LocalDate date) {
        if (status == RentalStatus.RETURNED) {
            throw new IllegalStateException("이미 반납된 대여 건입니다.");
        }
        if (date == null) date = LocalDate.now();
        return new Rental(id, bookId, memberId, rentedAt, dueAt, date, RentalStatus.RETURNED);
    }
}
