package io.github.bookrentalteam.bookrental.domain;

import io.github.bookrentalteam.bookrental.common.exception.ValidationException;

public class Book {
	private static long sequence = 0; // auto-increment

	private Long id;
	private String isbn;
	private String title;
	private String author;
	private int totalCopies;
	private int availableCopies;

	public Book(String isbn, String title, String author, int totalCopies) {
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

		this.id = ++sequence;
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.totalCopies = totalCopies;
		this.availableCopies = totalCopies;
	}

	// getter/setter
	public Long getId() {
		return id;
	}

	public String getIsbn() {
		return isbn;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public int getTotalCopies() {
		return totalCopies;
	}

	public int getAvailableCopies() {
		return availableCopies;
	}

	// 비즈니스 로직
	public boolean rent() {
		if (availableCopies > 0) {
			availableCopies--;
			return true;
		}
		return false;
	}

	public void returnBook() {
		if (availableCopies < totalCopies) {
			availableCopies++;
		}
	}
}
