package io.github.bookrentalteam.bookrental.repository;

import java.sql.Connection;
import java.util.List;

import io.github.bookrentalteam.bookrental.domain.Rental;
import io.github.bookrentalteam.bookrental.domain.RentalStatus;

public interface RentalRepository {
	long save(Connection conn, Rental rental);

	Rental findById(Connection conn, long rentalId);

	void updateStatus(Connection conn, long rentalId, RentalStatus status);

	List<Rental> findByMemberId(Connection conn, long memberId);

}
