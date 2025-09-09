package io.github.bookrentalteam.bookrental.domain;

import java.time.LocalDateTime;

import io.github.bookrentalteam.bookrental.common.exception.ValidationException;

public class Book {

	private Long id; // book_id (AUTO_INCREMENT, DB가 채움)
	private String isbn;
	private String title;
	private String author;
	private int totalCopies;
	private int availableCopies;
	private LocalDateTime createdAt; // created_at
	private LocalDateTime updatedAt; // updated_at

	/** JDBC 매핑용 기본 생성자 */
	public Book() {
	}

	/** 신규 등록 시 사용하는 생성자(검증 포함) */
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

		this.isbn = isbn.trim();
		this.title = title.trim();
		this.author = author.trim();
		this.totalCopies = totalCopies;
		this.availableCopies = totalCopies; // 최초 등록 시 전체 = 가용
	}

	// ===== 도메인 로직 =====
	public boolean canRent() {
		return availableCopies > 0;
	}

	/** 재고 1권 대여 */
	public boolean rent() {
		if (availableCopies <= 0) {
			return false;
		}
		availableCopies--;
		return true;
	}

	/** 재고 1권 반납 */
	public void returnBook() {
		if (availableCopies < totalCopies) {
			availableCopies++;
		}
	}

	// ===== Getter/Setter =====
	public Long getId() {
		return id;
	}

	/** DB INSERT 후 생성키 세팅용 */
	public void setId(Long id) {
		this.id = id;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public int getTotalCopies() {
		return totalCopies;
	}

	public void setTotalCopies(int totalCopies) {
		this.totalCopies = totalCopies;
	}

	public int getAvailableCopies() {
		return availableCopies;
	}

	public void setAvailableCopies(int availableCopies) {
		this.availableCopies = availableCopies;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
