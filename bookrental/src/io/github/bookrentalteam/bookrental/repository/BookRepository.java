package io.github.bookrentalteam.bookrental.repository;

import java.util.List;
import java.util.Optional;

import io.github.bookrentalteam.bookrental.domain.Book;

public interface BookRepository {
	void save(Book book);

	Optional<Book> findById(Long id);

	List<Book> findAll();

	void delete(Long id);
}
