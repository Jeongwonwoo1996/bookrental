package io.github.bookrentalteam.bookrental.repository;

import java.util.List;
import java.util.Optional;

import io.github.bookrentalteam.bookrental.domain.Member;

public interface MemberRepository {
	void save(Member member);

	Optional<Member> findById(Long id);

	Optional<Member> findByEmail(String email); // 로그인 시 활용

	List<Member> findAll();

	void delete(Long id);
}