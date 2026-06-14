package com.aleitox.transactions.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TransactionHierarchyRulesTest {

	@Test
	void validateParent_allowsNullParent() {
		assertThatCode(() -> TransactionHierarchyRules.validateParent(Map.of(), 10, null))
				.doesNotThrowAnyException();
	}

	@Test
	void validateParent_throwsWhenParentDoesNotExist() {
		assertThatThrownBy(() -> TransactionHierarchyRules.validateParent(Map.of(), 11, 10L))
				.isInstanceOf(InvalidParentException.class)
				.hasMessageContaining("Parent does not exist");
	}

	@Test
	void validateParent_throwsOnSelfParent() {
		Map<Long, Transaction> transactions = Map.of(10L, new Transaction(10, 100, "a", null));

		assertThatThrownBy(() -> TransactionHierarchyRules.validateParent(transactions, 10, 10L))
				.isInstanceOf(InvalidParentException.class)
				.hasMessageContaining("Cycle detected");
	}

	@Test
	void validateParent_throwsOnDirectCycle() {
		Map<Long, Transaction> transactions = Map.of(
				10L, new Transaction(10, 100, "a", null),
				11L, new Transaction(11, 100, "b", 10L));

		assertThatThrownBy(() -> TransactionHierarchyRules.validateParent(transactions, 10, 11L))
				.isInstanceOf(InvalidParentException.class)
				.hasMessageContaining("Cycle detected");
	}

	@Test
	void validateParent_throwsOnIndirectCycle() {
		Map<Long, Transaction> transactions = Map.of(
				10L, new Transaction(10, 100, "a", null),
				11L, new Transaction(11, 100, "b", 10L),
				12L, new Transaction(12, 100, "c", 11L));

		assertThatThrownBy(() -> TransactionHierarchyRules.validateParent(transactions, 10, 12L))
				.isInstanceOf(InvalidParentException.class)
				.hasMessageContaining("Cycle detected");
	}

	@Test
	void validateParent_allowsValidHierarchy() {
		Map<Long, Transaction> transactions = Map.of(
				10L, new Transaction(10, 100, "a", null),
				11L, new Transaction(11, 100, "b", 10L));

		assertThatCode(() -> TransactionHierarchyRules.validateParent(transactions, 12, 11L))
				.doesNotThrowAnyException();
	}

}
