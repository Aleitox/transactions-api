package com.aleitox.transactions.domain;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

	void save(Transaction transaction);

	Optional<Transaction> findById(long id);

	boolean exists(long id);

	List<Long> findIdsByType(String type);

	List<Long> findChildrenIds(long parentId);

	Optional<Double> sumTransitive(long transactionId);

}
