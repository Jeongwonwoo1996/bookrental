package io.github.bookrentalteam.bookrental.service.impl;

import java.time.LocalDate;
import java.util.List;

import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Rental;
import io.github.bookrentalteam.bookrental.repository.RentalRepository;
import io.github.bookrentalteam.bookrental.service.BookService;
import io.github.bookrentalteam.bookrental.service.RentalService;

public class RentalServiceImpl implements RentalService {

	private final RentalRepository rentalRepository;
	private final BookService bookService;

	public RentalServiceImpl(RentalRepository rentalRepository, BookService bookService) {
		this.rentalRepository = rentalRepository;
		this.bookService = bookService;
	}

	@Override
	public Rental rentBook(long bookId, Member member) {
		// TODO: BookServiceImpl 완성되면 도서 조회 및 재고 확인 로직 추가
		// Book book = bookService.findById(bookId)
		// .orElseThrow(() -> new IllegalArgumentException("도서를 찾을 수 없습니다."));
		//
		// if (!book.rent()) {
		// throw new IllegalStateException("대여 가능한 재고가 없습니다.");
		// }

		Rental rental = new Rental(bookId, member.getId());
		rentalRepository.save(rental);
		return rental;
	}

	@Override
	public Rental returnBook(long rentalId) {
		Rental rental = rentalRepository.findById(rentalId)
				.orElseThrow(() -> new IllegalArgumentException("해당 대여 기록을 찾을 수 없습니다."));
		rental.markReturned(LocalDate.now());
		rentalRepository.save(rental); // 상태 갱신
		return rental;
	}

	@Override
	public List<Rental> getRentalsByMember(Member member) {
		return rentalRepository.findByMemberId(member.getId());
	}

	@Override
	public void checkOverdueAndApplySuspension(Member member) {
		List<Rental> rentals = getRentalsByMember(member);
		for (Rental r : rentals) {
			if (r.isOverdue()) {
				System.out.printf("[경고] 회원 %s 연체 %d일 발생%n", member.getName(), r.overdueDays());
				// TODO: Member 엔티티에 제재(suspendUntil 등) 처리 추가 예정
			}
		}
	}

	@Override
	public Rental extendRental(long rentalId) {
		Rental rental = rentalRepository.findById(rentalId)
				.orElseThrow(() -> new IllegalArgumentException("해당 대여 기록을 찾을 수 없습니다"));

		// 연체된 도서가 하나라도 있으면 연장 불가
		List<Rental> rentals = rentalRepository.findByMemberId(rental.getMemberId());
		boolean hasOverdue = rentals.stream().anyMatch(Rental::isOverdue);
		if (hasOverdue) {
			throw new IllegalStateException("연체된 도서가 있어 연장할 수 없습니다.");
		}

		rental.extend(); // Rental의 연장 로직 실행
		rentalRepository.save(rental); // 상태 갱신
		return null;
	}

}
