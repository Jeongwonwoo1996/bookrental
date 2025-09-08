package io.github.bookrentalteam.bookrental.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.github.bookrentalteam.bookrental.common.exception.BusinessException;
import io.github.bookrentalteam.bookrental.common.exception.ValidationException;
import io.github.bookrentalteam.bookrental.domain.Book;
import io.github.bookrentalteam.bookrental.repository.BookRepository;
import io.github.bookrentalteam.bookrental.service.BookService;

public class BookServiceImpl implements BookService {

	private final BookRepository bookRepository;

	public BookServiceImpl(BookRepository bookRepository) {
		this.bookRepository = bookRepository;
	}

	/** 도서 등록 */
	@Override
	public Book registerBook(String isbn, String title, String author, int totalCopies) {
		// --- 입력 검증 ---
		if (isbn == null || isbn.isBlank()) {
			throw new ValidationException("ISBN은 필수입니다.");
		}
		if (title == null || title.isBlank()) {
			throw new ValidationException("제목은 필수입니다.");
		}
		if (author == null || author.isBlank()) {
			throw new ValidationException("저자는 필수입니다.");
		}
		if (totalCopies <= 0) {
			throw new ValidationException("총 권수는 1권 이상이어야 합니다.");
		}

		String tIsbn = isbn.trim();
		String tTitle = title.trim();
		String tAuthor = author.trim();

		// --- ISBN 중복 검사 (DB UNIQUE 보조) ---
		if (bookRepository.findByIsbn(tIsbn) != null) {
			throw new BusinessException("이미 존재하는 ISBN입니다: " + tIsbn);
		}

		// --- 도메인 생성 및 저장 ---
		Book book = new Book(tIsbn, tTitle, tAuthor, totalCopies);
		bookRepository.save(book); // DB가 PK 채워주면 book.setId(...)까지 반영됨
		return book;
	}

	/** 도서 목록 조회 */
	@Override
	public List<Book> listBooks() {
		return bookRepository.findAll();
	}

	/** 도서 검색 (제목, 저자, ISBN 키워드) */
	@Override
	public List<Book> searchBooks(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return listBooks(); // 검색어 없으면 전체
		}
		final String q = keyword.trim().toLowerCase(Locale.ROOT);

		// ※ 현재는 간단히 메모리 필터링.
		// 성능/정확도를 위해 추후 Repository에 DB 검색 메서드(searchByKeyword) 추가 권장.
		return bookRepository.findAll().stream()
				.filter(b -> (b.getTitle() != null && b.getTitle().toLowerCase(Locale.ROOT).contains(q))
						|| (b.getAuthor() != null && b.getAuthor().toLowerCase(Locale.ROOT).contains(q))
						|| (b.getIsbn() != null && b.getIsbn().toLowerCase(Locale.ROOT).contains(q)))
				.collect(Collectors.toList());
	}

	/** ID로 도서 단건 조회 */
	@Override
	public Book getBook(long id) {
		Book b = bookRepository.findById(id);
		if (b == null) {
			throw new BusinessException("ID에 해당하는 책을 찾을 수 없습니다: " + id);
		}
		return b;
	}
}
