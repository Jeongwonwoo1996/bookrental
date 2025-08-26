package io.github.bookrentalteam.bookrental.service.impl;

import java.util.List;

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
		return null;
	}

	@Override
	public List<Book> listBooks() {
		return null;
	}

	@Override
	public List<Book> searchBooks(String keyword) {
		return null;
	}

	@Override
	public Book getBook(long id) {
		return null;
	}

}
