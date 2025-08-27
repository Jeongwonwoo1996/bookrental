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

	// 책 등록 //
	@Override
	public Book registerBook(String isbn, String title, String author, int totalCopies) {
		if (isbn == null || isbn.isBlank()) {
			throw new ValidationException("ISBN은 필수입니다.");
		}
		// ISBN 중복 검사
		if (bookRepository.findByIsbn(isbn).isPresent()) {
			throw new IllegalStateException("이미 존재하는 ISBN입니다: " + isbn);
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
		return bookRepository.findAll().stream()
				.filter(book -> book.getTitle().toLowerCase().contains(keyword.toLowerCase())
						|| book.getAuthor().toLowerCase().contains(keyword.toLowerCase())
						|| book.getIsbn().toLowerCase().contains(keyword.toLowerCase()))
				.collect(Collectors.toList());
	}

	@Override
	public Book getBook(long id) {
		return bookRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 책을 찾을 수 없습니다: " + id));
	}

}
