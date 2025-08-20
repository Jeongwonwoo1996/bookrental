package io.github.bookrentalteam.bookrental;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Rental: 도서 대여 기록을 표현하는 불변(immutable) 레코드.
 *
 * 구성 필드
 * - id         : 대여 건 고유 식별자(UUID)
 * - bookId     : 대여한 도서의 식별자(UUID)
 * - memberId   : 대여자(회원) 식별자(UUID)
 * - rentedAt   : 대여일(기본값: 오늘)
 * - dueAt      : 반납 예정일(기본값: 대여일 + 14일)
 * - returnedAt : 실제 반납일(반납 전에는 null)
 * - status     : 대여 상태(REN​TED | RETURNED)
 *
 * 설계 메모
 * - Java record를 사용해 데이터 보관용으로 간결하게 정의.
 * - 상태 변경(반납 등)은 기존 인스턴스를 수정하지 않고 "새 인스턴스"를 만들어 반환.
 * - 생성자에서 필수값/기본값을 설정해 불변식(invariant)을 가능한 한 보장.
 *   (예: id 자동 생성, rentedAt/dueAt 기본값, status 기본값)
 *
 * 주의/개선 여지
 * - dueAt가 rentedAt보다 빠른 값으로 들어오는 외부 입력은 현재 차단하지 않으므로,
 *   필요하다면 (dueAt.isBefore(rentedAt)) 검증을 추가하는 것을 권장.
 * - returnedAt는 생성 시 null이어야 정상 흐름(대여 상태)이므로,
 *   생성자에서 status가 RETURNED일 때 returnedAt null 여부를 교차검증하는 것도 고려 가능.
 * - 동시성 환경에서는 원자성/트랜잭션 관리가 필요(여기선 단일 스레드 콘솔 가정).
 */
public record Rental(
        UUID id,
        UUID bookId,
        UUID memberId,
        LocalDate rentedAt,
        LocalDate dueAt,
        LocalDate returnedAt,
        RentalStatus status
) {

    /**
     * Compact constructor:
     * - 필드가 실제 필드에 할당되기 전에 유효성 검증/기본값 대입을 수행.
     * - 이 블록 안에서 파라미터(지역 변수) 값을 바꾸면, 그 값이 최종 필드로 저장됩니다.
     */
    public Rental {
        // id가 null이면 자동 발급
        if (id == null) id = UUID.randomUUID();

        // 필수 참조 값 검증
        if (bookId == null) throw new IllegalArgumentException("bookId 필수");
        if (memberId == null) throw new IllegalArgumentException("memberId 필수");

        // 날짜 기본값: 대여일 = 오늘, 예정일 = 대여일 + 14일
        if (rentedAt == null) rentedAt = LocalDate.now();
        if (dueAt == null)     dueAt   = rentedAt.plusDays(14);

        // 상태 기본값: RENTED
        if (status == null) status = RentalStatus.RENTED;

        // (선택 검증 예시) 예정일이 대여일보다 빠를 수 없음
        // if (dueAt.isBefore(rentedAt)) {
        //     throw new IllegalArgumentException("dueAt은 rentedAt 이후여야 합니다.");
        // }

        // (선택 검증 예시) status/returnedAt 일관성 체크
        // if (status == RentalStatus.RETURNED && returnedAt == null) {
        //     throw new IllegalArgumentException("반납 상태면 returnedAt이 필요합니다.");
        // }
    }

    /**
     * 반납 처리: 현재 Rental을 RETURNED 상태로 변경한 "새 Rental"을 반환.
     *
     * 규칙
     * - 이미 RETURNED 상태면 예외
     * - date가 null이면 오늘 날짜로 간주
     *
     * @param date 실제 반납일(null 허용 → 오늘로 처리)
     * @return 반환 처리된 새 Rental 인스턴스(불변 특성 유지)
     * @throws IllegalStateException 이미 반납된 대여 건일 때
     */
    public Rental markReturned(LocalDate date) {
        if (status == RentalStatus.RETURNED) {
            throw new IllegalStateException("이미 반납된 대여 건입니다.");
        }
        if (date == null) date = LocalDate.now();

        // (선택 검증 예시) 반납일이 대여일보다 빠를 수 없음
        // if (date.isBefore(rentedAt)) {
        //     throw new IllegalArgumentException("returnedAt은 rentedAt 이전일 수 없습니다.");
        // }

        // 새로운 인스턴스를 만들어 RETURNED 상태로 반환(원본은 유지)
        return new Rental(id, bookId, memberId, rentedAt, dueAt, date, RentalStatus.RETURNED);
    }
}
