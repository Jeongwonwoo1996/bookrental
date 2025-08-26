package io.github.bookrentalteam.bookrental.service.impl;

import java.util.List;

import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Rental;
import io.github.bookrentalteam.bookrental.service.RentalService;

public class RentalServiceImpl implements RentalService {

	@Override
	public Rental rentBook(long bookId, Member member) {
		return null;
	}

	@Override
	public Rental returnBook(long rentalId) {
		return null;
	}

	@Override
	public List<Rental> getRentalsByMember(Member member) {
		return null;
	}

	@Override
	public void checkOverdueAndApplySuspension(Member member) {
	}

}
