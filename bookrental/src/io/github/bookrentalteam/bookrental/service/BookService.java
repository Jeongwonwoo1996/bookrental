package io.github.bookrentalteam.bookrental.service;

import java.util.List;

import io.github.bookrentalteam.bookrental.domain.Book;

public interface BookService {
	/** 도서 등록 */
	Book registerBook(String isbn, String title, String author, int totalCopies);

	/** 도서 목록 조회 */
	List<Book> listBooks();

	/** 도서 검색 (제목, 저자, ISBN 키워드) */
	List<Book> searchBooks(String keyword);

	/** ID로 도서 단건 조회 */
	Book getBook(long id);
}
