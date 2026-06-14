package com.aleitox.transactions.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aleitox.transactions.domain.InvalidParentException;
import com.aleitox.transactions.domain.Transaction;

class InMemoryTransactionRepositoryTest {

	private InMemoryTransactionRepository repository;

	@BeforeEach
	void setUp() {
		repository = new InMemoryTransactionRepository();
	}

	@Test
	void findById_returnsEmptyWhenTransactionDoesNotExist() {
		assertThat(repository.findById(99)).isEmpty();
	}

	@Test
	void findById_returnsSavedTransaction() {
		Transaction transaction = new Transaction(10, 5000, "cars", null);

		repository.save(transaction);

		assertThat(repository.findById(10)).contains(transaction);
	}

	@Test
	void exists_returnsFalseWhenTransactionDoesNotExist() {
		assertThat(repository.exists(99)).isFalse();
	}

	@Test
	void exists_returnsTrueWhenTransactionWasSaved() {
		repository.save(new Transaction(10, 5000, "cars", null));

		assertThat(repository.exists(10)).isTrue();
	}

	@Test
	void findIdsByType_returnsEmptyListWhenNoTransactionsMatch() {
		repository.save(new Transaction(10, 5000, "cars", null));

		assertThat(repository.findIdsByType("shopping")).isEmpty();
	}

	@Test
	void findIdsByType_returnsIdsForMatchingType() {
		repository.save(new Transaction(10, 5000, "cars", null));
		repository.save(new Transaction(11, 10000, "shopping", 10L));
		repository.save(new Transaction(12, 5000, "shopping", 11L));

		assertThat(repository.findIdsByType("cars")).containsExactly(10L);
		assertThat(repository.findIdsByType("shopping")).containsExactlyInAnyOrder(11L, 12L);
	}

	@Test
	void findChildrenIds_returnsEmptyListWhenParentHasNoChildren() {
		repository.save(new Transaction(10, 5000, "cars", null));

		assertThat(repository.findChildrenIds(10)).isEmpty();
	}

	@Test
	void findChildrenIds_returnsOnlyDirectChildren() {
		repository.save(new Transaction(10, 5000, "cars", null));
		repository.save(new Transaction(11, 10000, "shopping", 10L));
		repository.save(new Transaction(12, 5000, "shopping", 11L));

		assertThat(repository.findChildrenIds(10)).containsExactly(11L);
		assertThat(repository.findChildrenIds(11)).containsExactly(12L);
		assertThat(repository.findChildrenIds(12)).isEmpty();
	}

	@Test
	void save_updatesTypeIndexWhenTypeChanges() {
		repository.save(new Transaction(10, 5000, "cars", null));

		repository.save(new Transaction(10, 5000, "shopping", null));

		assertThat(repository.findIdsByType("cars")).isEmpty();
		assertThat(repository.findIdsByType("shopping")).containsExactly(10L);
	}

	@Test
	void save_updatesChildrenIndexWhenParentChanges() {
		repository.save(new Transaction(10, 5000, "cars", null));
		repository.save(new Transaction(11, 10000, "shopping", 10L));
		repository.save(new Transaction(20, 2000, "cars", null));

		repository.save(new Transaction(11, 10000, "shopping", 20L));

		assertThat(repository.findChildrenIds(10)).isEmpty();
		assertThat(repository.findChildrenIds(20)).containsExactly(11L);
	}

	@Test
	void sumTransitive_returnsEmptyWhenTransactionDoesNotExist() {
		assertThat(repository.sumTransitive(99)).isEmpty();
	}

	@Test
	void sumTransitive_returnsOnlyRootAmountWhenThereAreNoDescendants() {
		repository.save(new Transaction(10, 5000, "cars", null));

		assertThat(repository.sumTransitive(10)).contains(5000.0);
	}

	@Test
	void sumTransitive_sumsRootAndAllDescendants() {
		repository.save(new Transaction(10, 5000, "cars", null));
		repository.save(new Transaction(11, 10000, "shopping", 10L));
		repository.save(new Transaction(12, 5000, "shopping", 11L));

		assertThat(repository.sumTransitive(10)).contains(20000.0);
		assertThat(repository.sumTransitive(11)).contains(15000.0);
		assertThat(repository.sumTransitive(12)).contains(5000.0);
	}

	@Test
	void save_throwsWhenParentDoesNotExist() {
		assertThatThrownBy(() -> repository.save(new Transaction(11, 100, "a", 10L)))
				.isInstanceOf(InvalidParentException.class)
				.hasMessageContaining("Parent does not exist");

		assertThat(repository.findById(11)).isEmpty();
	}

	@Test
	void save_throwsOnSelfParent() {
		repository.save(new Transaction(10, 100, "a", null));

		assertThatThrownBy(() -> repository.save(new Transaction(10, 100, "a", 10L)))
				.isInstanceOf(InvalidParentException.class)
				.hasMessageContaining("Cycle detected");

		assertThat(repository.findById(10)).contains(new Transaction(10, 100, "a", null));
	}

	@Test
	void save_throwsOnCycle() {
		repository.save(new Transaction(10, 100, "a", null));
		repository.save(new Transaction(11, 100, "b", 10L));

		assertThatThrownBy(() -> repository.save(new Transaction(10, 100, "a", 11L)))
				.isInstanceOf(InvalidParentException.class)
				.hasMessageContaining("Cycle detected");

		assertThat(repository.findById(10)).contains(new Transaction(10, 100, "a", null));
	}

	@Test
	void save_doesNotUpdateIndexesWhenValidationFails() {
		repository.save(new Transaction(10, 100, "a", null));

		assertThatThrownBy(() -> repository.save(new Transaction(11, 100, "b", 99L)))
				.isInstanceOf(InvalidParentException.class);

		assertThat(repository.findIdsByType("b")).isEmpty();
		assertThat(repository.findChildrenIds(10)).isEmpty();
	}

}
