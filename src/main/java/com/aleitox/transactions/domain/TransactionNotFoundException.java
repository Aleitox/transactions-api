package com.aleitox.transactions.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TransactionNotFoundException extends RuntimeException {

	public TransactionNotFoundException(long id) {
		super("Transaction not found: " + id);
	}

}
