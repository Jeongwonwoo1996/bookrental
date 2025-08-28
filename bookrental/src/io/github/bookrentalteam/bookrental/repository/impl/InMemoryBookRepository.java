package io.github.bookrentalteam.bookrental.repository.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.bookrentalteam.bookrental.domain.Book;
import io.github.bookrentalteam.bookrental.repository.BookRepository;

public class InMemoryBookRepository implements BookRepository {
	private final Map<Long, Book> store = new HashMap<>();

	@Override
	public void save(Book book) {
		store.put(book.getId(), book); // Book 생성 시 ID가 자동 부여되므로 그대로 사용
	}

	@Override
	public Optional<Book> findById(Long id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<Book> findAll() {
		return new ArrayList<>(store.values());
	}

	@Override
	public void delete(Long id) {
		store.remove(id);
	}

	@Override
	public Optional<Book> findByIsbn(String isbn) {
		return store.values().stream().filter(book -> book.getIsbn().equalsIgnoreCase(isbn)).findFirst();
	}

}
