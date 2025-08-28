package io.github.bookrentalteam.bookrental.service.impl;

import java.time.LocalDate;
import java.util.List;

import io.github.bookrentalteam.bookrental.domain.Book;
import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Rental;
import io.github.bookrentalteam.bookrental.domain.RentalStatus;
import io.github.bookrentalteam.bookrental.domain.Role;
import io.github.bookrentalteam.bookrental.repository.MemberRepository;
import io.github.bookrentalteam.bookrental.repository.RentalRepository;
import io.github.bookrentalteam.bookrental.service.BookService;
import io.github.bookrentalteam.bookrental.service.RentalService;

public class RentalServiceImpl implements RentalService {

	private final RentalRepository rentalRepository;
	private final MemberRepository memberRepository;
	private final BookService bookService;

	public RentalServiceImpl(RentalRepository rentalRepository, MemberRepository memberRepository,
			BookService bookService) {
		this.rentalRepository = rentalRepository;
		this.memberRepository = memberRepository;
		this.bookService = bookService;
	}

	@Override
	public Rental rentBook(long bookId, Member member) {
		// 제재 여부 확인
		if (member.isSuspended()) {
			throw new IllegalStateException("현재 대여 정지 상태입니다. 해제일: " + member.getSuspendUntil());
		}

		// 연체 도서 여부 확인
		boolean hasOverdue = rentalRepository.findByMemberId(member.getId()).stream().anyMatch(Rental::isOverdue);
		if (hasOverdue) {
			throw new IllegalStateException("연체된 도서가 있어 대여할 수 없습니다.");
		}

		// 일반 회원은 대여 권수 제한 (최대 7권)
		if (member.getRole() == Role.USER) {
			long rentedCount = rentalRepository.findByMemberId(member.getId()).stream()
					.filter(r -> r.getStatus() == RentalStatus.RENTED) // 아직 반납 안 한 도서만 카운트
					.count();
			if (rentedCount >= 7) {
				throw new IllegalStateException("일반 회원은 동시에 최대 7권까지 대여할 수 있습니다.");
			}
		}

		// 도서 조회 및 재고 확인
		Book book = bookService.getBook(bookId);
		if (!book.rent()) {
			throw new IllegalStateException("대여 가능한 재고가 없습니다.");
		}

		// 대여 생성
		Rental rental = new Rental(bookId, member.getId());
		rentalRepository.save(rental);
		return rental;
	}

	@Override
	public Rental returnBook(long rentalId) {
		Rental rental = rentalRepository.findById(rentalId)
				.orElseThrow(() -> new IllegalArgumentException("해당 대여 기록을 찾을 수 없습니다."));

		// 반납 전에 연체 여부 확인 → 연체 일수만큼 정지
		if (rental.isOverdue()) {
			long overdueDays = rental.overdueDays();
			memberRepository.findById(rental.getMemberId()).ifPresent(m -> {
				m.suspend((int) overdueDays);
				System.out.printf("[제재] 회원 %s 연체 %d일 → %d일 대여 정지 (해제일: %s)%n", m.getName(), overdueDays, overdueDays,
						m.getSuspendUntil());
			});
		}

		// 반납 처리
		rental.markReturned(LocalDate.now());

		// 도서 재고 복원
		Book book = bookService.getBook(rental.getBookId());
		book.returnBook();

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
				long days = r.overdueDays();
				member.suspend((int) days); // ✅ 연체 일수만큼 정지
				System.out.printf("[경고] 회원 %s 연체 %d일 → %d일 대여 정지%n", member.getName(), days, days);

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

		// 제재 여부 확인
		if (memberRepository.findById(rental.getMemberId()).get().isSuspended()) {
			throw new IllegalStateException("대여 정지 상태에서는 연장할 수 없습니다.");
		}

		rental.extend(); // Rental의 연장 로직 실행
		rentalRepository.save(rental); // 상태 갱신
		return rental;
	}

}
