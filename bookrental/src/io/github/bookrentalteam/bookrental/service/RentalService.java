package io.github.bookrentalteam.bookrental.service;

import java.util.List;

import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Rental;

public interface RentalService {

	/** 여러 권 대여: 헤더 1건 생성 + 상세 N건 + 재고 N건 차감 */
	Rental rentBooks(Member member, List<Long> bookIds);

	/** 선택 도서 다건 반납 (상세 상태 RETURNED, 재고 복구, 헤더 상태 갱신) */
	int returnBooks(long rentalId, List<Long> bookIds);

	/** 선택 도서 다건 연장(7일) — 규약: 상세가 RENTED이고, 연체 아님, 연장 1회 미만 */
	int extendBooks(long rentalId, List<Long> bookIds);

	/** 특정 회원의 대여 헤더 목록 */
	List<Rental> getRentalsByMember(long memberId);

	/** 회원의 연체 상태 점검 및 제재 적용 */
	void checkOverdueAndApplySuspension(long memberId);

	/** 연체 보유 여부 단순 조회(메뉴 진입 차단용) */
	boolean existsOverdueByMember(long memberId);

	// ✅ 추가: 회원 기준 bookId만 받아 반납/연장 처리
	int returnBooksByBookIds(long memberId, List<Long> rawBookIds);

	int extendBooksByBookIds(long memberId, List<Long> rawBookIds);
}
