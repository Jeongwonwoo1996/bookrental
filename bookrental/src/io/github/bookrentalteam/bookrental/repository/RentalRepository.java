package io.github.bookrentalteam.bookrental.repository;

import java.util.List;
import java.util.Optional;

import io.github.bookrentalteam.bookrental.domain.Rental;

public interface RentalRepository {
	void save(Rental rental);

	Optional<Rental> findById(Long id);

	List<Rental> findAll();

	List<Rental> findByMemberId(Long memberId); // 내 대여목록 조회용

	void delete(Long id);
}
