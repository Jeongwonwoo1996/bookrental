package io.github.bookrentalteam.bookrental.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import io.github.bookrentalteam.bookrental.common.exception.ValidationException;
import io.github.bookrentalteam.bookrental.domain.Book;
import io.github.bookrentalteam.bookrental.repository.BookRepository;
import io.github.bookrentalteam.bookrental.service.BookService;

public class BookServiceImpl implements BookService {

	private final BookRepository bookRepository;

	public BookServiceImpl(BookRepository bookRepository) {
		this.bookRepository = bookRepository;
	}

	@Override
	public Book registerBook(String isbn, String title, String author, int totalCopies) {
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

		Book book = new Book(isbn, title, author, totalCopies);
		bookRepository.save(book);
		return book;
	}

	@Override
	public List<Book> listBooks() {
		return bookRepository.findAll();
	}

	@Override
	public List<Book> searchBooks(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return listBooks(); // 검색어 없으면 전체 목록 반환
		}

		String lower = keyword.toLowerCase();
		return bookRepository
				.findAll().stream().filter(b -> b.getTitle().toLowerCase().contains(lower)
						|| b.getAuthor().toLowerCase().contains(lower) || b.getIsbn().toLowerCase().contains(lower))
				.collect(Collectors.toList());
	}

	@Override
	public Book getBook(long id) {
		return bookRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 도서를 찾을 수 없습니다."));
	}

}
