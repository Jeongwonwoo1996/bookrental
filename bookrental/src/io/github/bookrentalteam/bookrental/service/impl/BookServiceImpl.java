package io.github.bookrentalteam.bookrental.service.impl;

import java.util.List;
import java.util.stream.Collectors;

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

		// ISBN 중복 검사
		if (bookRepository.findByIsbn(isbn).isPresent()) {
			throw new IllegalStateException("이미 존재하는 ISBN입니다: " + isbn);
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
		// 모든 책을 가져옴
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
