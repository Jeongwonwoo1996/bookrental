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

	@Override
	public Book registerBook(String isbn, String title, String author, int totalCopies) {

		// 새로운 Book 객체 생성
		Book book = new Book(isbn, title, author, totalCopies);
		// Repository를 통해 저장
		bookRepository.save(book);

		// 생성된 Book 객체 반환
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
				// 제목(title) 또는 저자(author)에 키워드가 포함되어 있는지 필터링 (대소문자 무시)
				.filter(book -> book.getTitle().toLowerCase().contains(keyword.toLowerCase())
						|| book.getAuthor().toLowerCase().contains(keyword.toLowerCase()))
				// 결과를 리스트로 수집하여 반환
				.collect(Collectors.toList());
	}

	@Override
	public Book getBook(long id) {
		// Repository를 통해 ID로 책을 찾고, 없으면 예외 발생
		return bookRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 책을 찾을 수 없습니다: " + id));
	}

}
