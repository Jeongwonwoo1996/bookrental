package io.github.bookrentalteam.bookrental.repository;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import io.github.bookrentalteam.bookrental.domain.DetailStatus;
import io.github.bookrentalteam.bookrental.domain.RentalDetail;

public interface RentalDetailRepository {
	void saveAll(Connection conn, long rentalId, List<RentalDetail> details);

	List<RentalDetail> findByRentalId(Connection conn, long rentalId);

	// 상태 일괄 변경(선택된 책들만)
	int updateStatusBatch(Connection conn, long rentalId, List<Long> bookIds, DetailStatus status,
			LocalDate returnedAt);

	int extendBatch7d(Connection conn, long rentalId, List<Long> bookIds); // due_at += 7, extension_count += 1

	// 집계용
	int countActive(Connection conn, long rentalId); // RENTED/OVERDUE/LOST 개수

	int countReturned(Connection conn, long rentalId);

	/**
	 * 회원이 현재 '활성'(RENTED/OVERDUE/LOST) 상태로 보유 중인 도서 중, 전달된 bookIds와 겹치는 bookId 목록을
	 * 반환합니다.
	 */
	List<Long> findActiveBookIdsByMemberAndBookIds(Connection conn, long memberId, List<Long> bookIds);
}
