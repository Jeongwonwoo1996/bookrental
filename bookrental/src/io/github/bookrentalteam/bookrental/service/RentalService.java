package io.github.bookrentalteam.bookrental.service;

import java.util.List;

import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Rental;

public interface RentalService {
	/** 도서 대여 */
	Rental rentBook(long bookId, Member member);

	/** 도서 반납 */
	Rental returnBook(long rentalId);

	/** 특정 회원의 대여 이력 조회 */
	List<Rental> getRentalsByMember(Member member);

	/** 연체 여부 검사 및 제재 처리 */
	void checkOverdueAndApplySuspension(Member member);

	/** 대여 연장 */
	Rental extendRental(long rentalId);
}
