package io.github.bookrentalteam.bookrental.repository;

import java.sql.Connection;
import java.time.LocalDate;

import io.github.bookrentalteam.bookrental.domain.Member;

public interface MemberRepository {
	long save(Member m);

	Member findById(long id);

	Member findByEmail(String email);

	// 인터페이스
	void updateSuspendUntil(Connection conn, long memberId, LocalDate suspendUntil);

}
