package io.github.bookrentalteam.bookrental.domain;

import io.github.bookrentalteam.bookrental.common.exception.ValidationException;

/**
 * 도서 엔티티 (불변, auto-increment ID)
 */
public record Book(long id, String isbn, String title, String author, int totalCopies, int availableCopies) {
	private static long sequence = 0; // auto-increment

	public Book {
		if (isbn == null || isbn.isBlank()) {
			throw new ValidationException("ISBN은 필수입니다.");
		}
		if (title == null || title.isBlank()) {
			throw new ValidationException("제목은 필수입니다.");
		}
		if (author == null || author.isBlank()) {
			throw new ValidationException("저자는 필수입니다.");
		}
		if (totalCopies < 0) {
			throw new ValidationException("총 권수는 0 이상이어야 합니다.");
		}
		if (availableCopies < 0 || availableCopies > totalCopies) {
			throw new ValidationException("대여 가능 권수가 유효하지 않습니다.");
		}
	}

	// 자동 ID 발급용 생성자
	public Book(String isbn, String title, String author, int total) {
		this(++sequence, isbn, title, author, total, total);
	}
}
