package io.github.bookrentalteam.bookrental;

/**
 * RentalStatus: 도서 대여 상태를 나타내는 열거형(enum).
 *
 * 특징
 * - 열거형(enum)은 미리 정의된 상수 값들의 집합을 표현할 때 사용.
 * - 이 프로젝트에서는 도서 대여의 상태를 "RENTED" 또는 "RETURNED" 두 가지로 제한.
 * - 상태를 문자열로 처리하는 대신 enum을 쓰면
 *   - 오타/잘못된 값 입력 방지
 *   - 코드 가독성 향상
 *   - switch/case, 비교 연산에서 안전성 확보
 *
 * 구성 값
 * - RENTED   : 현재 대여 중 상태
 * - RETURNED : 반납 완료 상태
 *
 * 예시 사용
 * ------------------------
 * RentalStatus s = RentalStatus.RENTED;
 * if (s == RentalStatus.RETURNED) {
 *     System.out.println("이미 반납된 상태입니다.");
 * }
 *
 * switch (s) {
 *     case RENTED -> System.out.println("대여 중");
 *     case RETURNED -> System.out.println("반납됨");
 * }
 * ------------------------
 *
 * 확장 가능성
 * - 향후 상태를 추가할 수 있음 (예: 연체 상태 = OVERDUE, 예약 상태 = RESERVED).
 * - 이때 enum은 고정된 상수 집합이므로 상태 관리 로직 전체에 컴파일 타임에서 오류를 잡을 수 있음.
 */
public enum RentalStatus {
    RENTED,   // 대여 중
    RETURNED  // 반납 완료
}