package io.github.bookrentalteam.bookrental;

import java.util.UUID;

/**
 * 불변(immutable) 도서 엔티티.
 * - Java record를 사용하므로 모든 필드는 생성 시점에만 설정되며 이후 변경되지 않습니다.
 * - 재고 증감 시에는 기존 인스턴스를 수정하지 않고, 값이 반영된 "새로운 Book" 인스턴스를 반환합니다.
 *
 * 불변식(Invariants)
 * - totalCopies >= 0
 * - availableCopies >= 0
 * - availableCopies <= totalCopies
 * - isbn, title, author는 공백이 아니어야 함
 * - id가 null로 들어오면 UUID.randomUUID()로 자동 생성
 */
public record Book(
        UUID id,            // 도서 고유 식별자
        String isbn,        // 국제 표준 도서 번호(형식 검증은 단순 공백/널만 체크)
        String title,       // 책 제목
        String author,      // 저자
        int totalCopies,    // 총 보유 권수
        int availableCopies // 현재 대여 가능(재고) 권수
) {
    /**
     * Compact constructor
     * - record의 압축 생성자: 필드에 할당되기 전에 유효성 검증 및 기본값 설정을 수행합니다.
     * - 주의: 이 블록 안에서 파라미터 변수(id, isbn, ...)를 수정하면
     *         그 값이 최종적으로 필드에 할당됩니다.
     */
    public Book {
        // id가 null이면 새로운 UUID 발급
        if (id == null) id = UUID.randomUUID();

        // 문자열 기본 검증(널/공백)
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("ISBN은(는) 필수입니다.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은(는) 필수입니다.");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("저자는(은) 필수입니다.");
        }

        // 수량 검증
        if (totalCopies < 0) {
            throw new IllegalArgumentException("총권수는 0 이상이어야 합니다.");
        }
        if (availableCopies < 0) {
            throw new IllegalArgumentException("대여 가능 권수는 0 이상이어야 합니다.");
        }
        if (availableCopies > totalCopies) {
            throw new IllegalArgumentException("대여 가능 권수는 총권수를 초과할 수 없습니다.");
        }
    }

    /**
     * 재고 증가: 입고/비치 등의 사유로 총권수와 대여 가능 권수를 동일하게 n만큼 증가시킵니다.
     *
     * @param n 증가시킬 수량(1 이상)
     * @return 수량이 증가된 새로운 Book 인스턴스(원본은 불변)
     * @throws IllegalArgumentException n < 1 인 경우
     */
    public Book increase(int n) {
        if (n < 1) throw new IllegalArgumentException("증가 수량은 1 이상이어야 합니다.");
        // 불변 레코드 특성상 필드 값을 변경하지 않고, 변경 반영된 새 Book을 생성해서 반환
        return new Book(id, isbn, title, author, totalCopies + n, availableCopies + n);
    }

    /**
     * 재고 감소: 폐기/분실/이관 등의 사유로 총권수와 대여 가능 권수를 동일하게 n만큼 감소시킵니다.
     * - 현재 재고(availableCopies)가 n보다 적으면 줄일 수 없습니다.
     * - 불변식(availableCopies ≤ totalCopies)을 유지하기 위해 total과 available을 함께 감소시킵니다.
     *
     * @param n 감소시킬 수량(1 이상)
     * @return 수량이 감소된 새로운 Book 인스턴스(원본은 불변)
     * @throws IllegalArgumentException n < 1 인 경우
     * @throws IllegalStateException    현재 재고가 부족하여 감축할 수 없는 경우(availableCopies < n)
     */
    public Book decrease(int n) {
        if (n < 1) throw new IllegalArgumentException("감소 수량은 1 이상이어야 합니다.");
        if (availableCopies < n) throw new IllegalStateException("availableCopies가 부족하여 감축할 수 없습니다.");

        // availableCopies >= n이고, 항상 available ≤ total이라는 불변식이 보장되므로
        // totalCopies - n이 음수가 되는 상황은 발생하지 않습니다.
        return new Book(id, isbn, title, author, totalCopies - n, availableCopies - n);
    }
}
