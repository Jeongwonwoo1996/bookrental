package io.github.bookrentalteam.bookrental.repository;

import java.sql.Connection;
import java.util.List;

import io.github.bookrentalteam.bookrental.domain.Book;

public interface BookRepository {
	long save(Book book);

	Book findById(long id);

	Book findByIsbn(String isbn);

	List<Book> findAll();

	void updateCopies(long bookId, int totalCopies, int availableCopies);

	// 트랜잭션용 (대여/반납 시)
	int decreaseAvailable(Connection conn, long bookId); // 1 감소(재고 있으면 1 반환)

	int increaseAvailable(Connection conn, long bookId); // 1 증가

	int lockAndGetAvailable(Connection conn, long bookId); // SELECT ... FOR UPDATE
}
