package io.github.bookrentalteam.bookrental.repository.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.bookrentalteam.bookrental.domain.Rental;
import io.github.bookrentalteam.bookrental.repository.RentalRepository;

public class InMemoryRentalRepository implements RentalRepository {
	private final Map<Long, Rental> store = new HashMap<>();

	@Override
	public void save(Rental rental) {
		store.put(rental.getId(), rental); // Rental 생성자에서 ID 자동 생성됨
	}

	@Override
	public Optional<Rental> findById(Long id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<Rental> findAll() {
		return new ArrayList<>(store.values());
	}

	@Override
	public List<Rental> findByMemberId(Long memberId) {
		List<Rental> rentals = new ArrayList<>();
		for (Rental rental : store.values()) {
			if (rental.getMemberId().equals(memberId)) {
				rentals.add(rental);
			}
		}
		return rentals;
	}

	@Override
	public void delete(Long id) {
		store.remove(id);
	}
}
