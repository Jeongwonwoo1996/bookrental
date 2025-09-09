package io.github.bookrentalteam.bookrental.service.impl;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.bookrentalteam.bookrental.common.exception.BusinessException;
import io.github.bookrentalteam.bookrental.common.exception.ValidationException;
import io.github.bookrentalteam.bookrental.config.ConnectionManager;
import io.github.bookrentalteam.bookrental.domain.DetailStatus;
import io.github.bookrentalteam.bookrental.domain.Member;
import io.github.bookrentalteam.bookrental.domain.Rental;
import io.github.bookrentalteam.bookrental.domain.RentalDetail;
import io.github.bookrentalteam.bookrental.domain.RentalStatus;
import io.github.bookrentalteam.bookrental.domain.Role;
import io.github.bookrentalteam.bookrental.repository.BookRepository;
import io.github.bookrentalteam.bookrental.repository.MemberRepository;
import io.github.bookrentalteam.bookrental.repository.RentalDetailRepository;
import io.github.bookrentalteam.bookrental.repository.RentalRepository;
import io.github.bookrentalteam.bookrental.service.RentalService;

public class RentalServiceImpl implements RentalService {

	private final RentalRepository rentalRepo;
	private final RentalDetailRepository detailRepo;
	private final BookRepository bookRepo;
	private final MemberRepository memberRepo;

	public RentalServiceImpl(RentalRepository rentalRepo, RentalDetailRepository detailRepo, BookRepository bookRepo,
			MemberRepository memberRepo) {
		this.rentalRepo = rentalRepo;
		this.detailRepo = detailRepo;
		this.bookRepo = bookRepo;
		this.memberRepo = memberRepo;
	}

	// ----------------------
	// 대여(다건)
	// ----------------------
	@Override
	public Rental rentBooks(Member member, List<Long> rawBookIds) {
		if (member == null || member.getId() == null) {
			throw new ValidationException("회원 정보가 유효하지 않습니다.");
		}
		if (rawBookIds == null || rawBookIds.isEmpty()) {
			throw new ValidationException("대여할 도서를 1권 이상 선택해 주세요.");
		}
		if (member.isSuspended()) {
			throw new BusinessException("현재 대여 정지 상태입니다. 해제일: " + member.getSuspendUntil());
		}

		// 입력 bookId 중복 제거(같은 거래 내 동일 도서 중복 금지: (rental_id, book_id) PK)
		List<Long> bookIds = new ArrayList<>(new LinkedHashSet<>(rawBookIds));

		try (Connection conn = ConnectionManager.getConnection()) {
			conn.setAutoCommit(false);
			try {
				// 0) 동일 도서 중복 대여 사전 차단 (이미 보유 중인 도서)
				List<Long> activeDup = detailRepo.findActiveBookIdsByMemberAndBookIds(conn, member.getId(), bookIds);
				if (!activeDup.isEmpty()) {
					throw new BusinessException("이미 대여 중인 도서가 포함되어 대여할 수 없습니다: " + activeDup);
				}

				// 1) 연체 보유 시 대여 차단(빠른 존재 조회)
				if (detailRepo.existsOverdueByMemberId(conn, member.getId())) {
					throw new BusinessException("연체 중인 도서가 있어 대여할 수 없습니다.");
				}

				// 활성 권수 계산 (OPEN 헤더 기준)
				List<Rental> headers = rentalRepo.findByMemberId(conn, member.getId()).stream()
						.filter(r -> r.getRentalStatus() == RentalStatus.OPEN).collect(Collectors.toList());
				int activeCount = 0;
				for (Rental r : headers) {
					var details = detailRepo.findByRentalId(conn, r.getId());
					for (RentalDetail d : details) {
						if (d.getDetailStatus() == DetailStatus.RENTED || d.getDetailStatus() == DetailStatus.OVERDUE
								|| d.getDetailStatus() == DetailStatus.LOST) {
							activeCount++;
						}
					}
				}
				if (member.getRole() == Role.USER) {
					if (activeCount + bookIds.size() > 7) {
						throw new BusinessException("일반 회원은 동시에 최대 7권까지 대여할 수 있습니다. (현재: " + activeCount + "권)");
					}
				}

				// 2) 헤더 생성
				Rental header = new Rental(member.getId()); // 기본 OPEN, rentedAt=now
				long rentalId = rentalRepo.save(conn, header);

				// 3) 각 도서 재고 잠금/차감 + 상세 생성
				List<RentalDetail> details = new ArrayList<>();
				for (Long bookId : bookIds) {
					// 잠금 후 재고 확인/차감
					int avail = bookRepo.lockAndGetAvailable(conn, bookId);
					if (avail <= 0) {
						throw new BusinessException("재고 부족으로 대여할 수 없습니다. (bookId=" + bookId + ")");
					}
					int dec = bookRepo.decreaseAvailable(conn, bookId);
					if (dec == 0) {
						throw new BusinessException("재고 차감 실패: 동시성 충돌 (bookId=" + bookId + ")");
					}
					// 상세 엔티티
					details.add(RentalDetail.of(rentalId, bookId));
				}
				// 4) 상세 일괄 저장
				detailRepo.saveAll(conn, rentalId, details);

				conn.commit();
				return header;
			} catch (Exception e) {
				conn.rollback();
				throw e;
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (Exception e) {
			throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
		}
	}

	// ----------------------
	// 반납(다건)
	// ----------------------
	@Override
	public int returnBooks(long rentalId, List<Long> rawBookIds) {
		if (rentalId <= 0) {
			throw new ValidationException("rentalId가 유효하지 않습니다.");
		}
		if (rawBookIds == null || rawBookIds.isEmpty()) {
			throw new ValidationException("반납할 도서를 선택해 주세요.");
		}
		List<Long> bookIds = new ArrayList<>(new LinkedHashSet<>(rawBookIds));

		try (Connection conn = ConnectionManager.getConnection()) {
			conn.setAutoCommit(false);
			try {
				Rental header = rentalRepo.findById(conn, rentalId);
				if (header == null) {
					throw new BusinessException("대여 기록을 찾을 수 없습니다: " + rentalId);
				}

				// 현재 상세 로딩 후, 반납 대상만 선별(현재 상태가 RENTED/OVERDUE)
				var allDetails = detailRepo.findByRentalId(conn, rentalId);
				Map<Long, RentalDetail> byBook = allDetails.stream()
						.collect(Collectors.toMap(RentalDetail::getBookId, d -> d));
				List<Long> targets = new ArrayList<>();
				LocalDate today = LocalDate.now();
				int suspendDaysToAdd = 0;

				for (Long bookId : bookIds) {
					RentalDetail d = byBook.get(bookId);
					if (d == null) {
						throw new BusinessException("해당 거래에 없는 도서입니다: bookId=" + bookId);
					}
					if (d.getDetailStatus() == DetailStatus.RENTED || d.getDetailStatus() == DetailStatus.OVERDUE) {
						targets.add(bookId);
						// 연체였다면 제재 일수 가산
						if (d.getDueAt().isBefore(today)) {
							suspendDaysToAdd += (int) ChronoUnit.DAYS.between(d.getDueAt(), today);
						}
					}
				}
				if (targets.isEmpty()) {
					conn.rollback();
					return 0;
				}

				// 상세 상태 RETURNED + returned_at=오늘
				detailRepo.updateStatusBatch(conn, rentalId, targets, DetailStatus.RETURNED, today);

				// 재고 복구
				for (Long bookId : targets) {
					bookRepo.increaseAvailable(conn, bookId);
				}

				// 연체 제재 적용(해당 rental의 member에게)
				if (suspendDaysToAdd > 0) {
					// memberId 조회
					long memberId = header.getMemberId();
					var member = memberRepo.findById(memberId);
					if (member != null) {
						member.suspend(suspendDaysToAdd); // 도메인 갱신
						// DB 반영(트랜잭션 내 적용)
						memberRepo.updateSuspendUntil(conn, memberId, member.getSuspendUntil());
					}
				}

				// 헤더 상태 갱신(활성 상세 카운트 기반)
				int active = detailRepo.countActive(conn, rentalId);
				rentalRepo.updateStatus(conn, rentalId, active == 0 ? RentalStatus.CLOSED : RentalStatus.OPEN);

				conn.commit();
				return targets.size();
			} catch (Exception e) {
				conn.rollback();
				throw e;
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (Exception e) {
			throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
		}
	}

	// ----------------------
	// 연장(다건, +7일)
	// ----------------------
	@Override
	public int extendBooks(long rentalId, List<Long> rawBookIds) {
		if (rentalId <= 0) {
			throw new ValidationException("rentalId가 유효하지 않습니다.");
		}
		if (rawBookIds == null || rawBookIds.isEmpty()) {
			throw new ValidationException("연장할 도서를 선택해 주세요.");
		}
		List<Long> bookIds = new ArrayList<>(new LinkedHashSet<>(rawBookIds));

		try (Connection conn = ConnectionManager.getConnection()) {
			conn.setAutoCommit(false);
			try {
				Rental header = rentalRepo.findById(conn, rentalId);
				if (header == null) {
					throw new BusinessException("대여 기록을 찾을 수 없습니다: " + rentalId);
				}

				// 회원 연체 보유 시 연장 불가(빠른 조회)
				if (detailRepo.existsOverdueByMemberId(conn, header.getMemberId())) {
					throw new BusinessException("연체된 도서가 있어 연장할 수 없습니다.");
				}

				// 상세 현재 상태 확인 후, 규약에 맞는 대상만 남김
				var details = detailRepo.findByRentalId(conn, rentalId).stream()
						.collect(Collectors.toMap(RentalDetail::getBookId, d -> d));

				List<Long> eligible = new ArrayList<>();
				LocalDate today = LocalDate.now();
				for (Long bookId : bookIds) {
					RentalDetail d = details.get(bookId);
					if (d == null) {
						throw new BusinessException("해당 거래에 없는 도서입니다: bookId=" + bookId);
					}
					if (d.getDetailStatus() == DetailStatus.RENTED && !d.getDueAt().isBefore(today)
							&& d.getExtensionCount() < 1) {
						eligible.add(bookId);
					}
				}
				if (eligible.isEmpty()) {
					conn.rollback();
					return 0;
				}

				int updated = detailRepo.extendBatch7d(conn, rentalId, eligible);

				conn.commit();
				return updated;
			} catch (Exception e) {
				conn.rollback();
				throw e;
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (Exception e) {
			throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
		}
	}

	// ----------------------
	// 조회/유틸
	// ----------------------
	@Override
	public List<Rental> getRentalsByMember(long memberId) {
		try (Connection conn = ConnectionManager.getConnection()) {
			return rentalRepo.findByMemberId(conn, memberId);
		} catch (Exception e) {
			throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
		}
	}

	@Override
	public void checkOverdueAndApplySuspension(long memberId) {
		try (Connection conn = ConnectionManager.getConnection()) {
			conn.setAutoCommit(false);
			try {
				var headers = rentalRepo.findByMemberId(conn, memberId);
				LocalDate today = LocalDate.now();
				int addDays = 0;
				for (Rental r : headers) {
					for (RentalDetail d : detailRepo.findByRentalId(conn, r.getId())) {
						if ((d.getDetailStatus() == DetailStatus.RENTED || d.getDetailStatus() == DetailStatus.OVERDUE)
								&& d.getDueAt().isBefore(today)) {
							addDays += (int) ChronoUnit.DAYS.between(d.getDueAt(), today);
						}
					}
				}
				if (addDays > 0) {
					var m = memberRepo.findById(memberId);
					if (m != null) {
						m.suspend(addDays);
						memberRepo.updateSuspendUntil(conn, memberId, m.getSuspendUntil());
					}
				}
				conn.commit();
			} catch (Exception e) {
				conn.rollback();
				throw e;
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (Exception e) {
			throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
		}
	}

	@Override
	public boolean existsOverdueByMember(long memberId) {
		try (Connection conn = ConnectionManager.getConnection()) {
			return detailRepo.existsOverdueByMemberId(conn, memberId);
		} catch (Exception e) {
			throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
		}
	}
}
