package com.aleitox.transactions.domain;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

	/**
	 * Persists the transaction and updates indexes atomically.
	 * Validates that {@code parentId} refers to an existing transaction and would not
	 * create a cycle; validation runs under the same write lock as persistence.
	 *
	 * @throws InvalidParentException if the parent is missing or would create a cycle
	 */
	void save(Transaction transaction);

	Optional<Transaction> findById(long id);

	boolean exists(long id);

	List<Long> findIdsByType(String type);

	List<Long> findChildrenIds(long parentId);

	Optional<Double> sumTransitive(long transactionId);

}
