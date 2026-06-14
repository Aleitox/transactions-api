package com.aleitox.transactions.api;

import java.util.Locale;

final class TransactionTypeNormalizer {

	private TransactionTypeNormalizer() {
	}

	static String normalize(String rawType) {
		String normalized = rawType.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("type must not be empty");
		}
		return normalized;
	}

}
