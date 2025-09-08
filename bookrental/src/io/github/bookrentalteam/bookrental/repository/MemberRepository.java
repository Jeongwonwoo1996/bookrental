package io.github.bookrentalteam.bookrental.repository;

import io.github.bookrentalteam.bookrental.domain.Member;

public interface MemberRepository {
	long save(Member m);

	Member findById(long id);

	Member findByEmail(String email);
}
