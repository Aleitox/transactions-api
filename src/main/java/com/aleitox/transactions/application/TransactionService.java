package com.aleitox.transactions.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.aleitox.transactions.domain.Transaction;
import com.aleitox.transactions.domain.TransactionRepository;

@Service
public class TransactionService {

	private final TransactionRepository repository;

	public TransactionService(TransactionRepository repository) {
		this.repository = repository;
	}

	public void save(long id, double amount, String normalizedType, Long parentId) {
		repository.save(new Transaction(id, amount, normalizedType, parentId));
	}

	public List<Long> findIdsByType(String normalizedType) {
		return repository.findIdsByType(normalizedType);
	}

}
