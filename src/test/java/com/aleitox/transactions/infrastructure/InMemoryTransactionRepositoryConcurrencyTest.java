package com.aleitox.transactions.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aleitox.transactions.domain.InvalidParentException;
import com.aleitox.transactions.domain.Transaction;

class InMemoryTransactionRepositoryConcurrencyTest {

	private static final int THREAD_COUNT = 100;

	private InMemoryTransactionRepository repository;

	@BeforeEach
	void setUp() {
		repository = new InMemoryTransactionRepository();
	}

	@Test
	void save_doesNotPersistTransactionsWithMissingParentUnderConcurrentWrites() throws Exception {
		repository.save(new Transaction(10, 100, "root", null));

		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch workersDone = new CountDownLatch(THREAD_COUNT);
		AtomicInteger rejected = new AtomicInteger();

		for (int i = 0; i < THREAD_COUNT; i++) {
			long childId = 1000L + i;
			Thread worker = new Thread(() -> {
				try {
					start.await();
					repository.save(new Transaction(childId, 1, "child", 99L));
				}
				catch (InvalidParentException exception) {
					rejected.incrementAndGet();
				}
				catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
				}
				finally {
					workersDone.countDown();
				}
			});
			worker.start();
		}

		Thread mutator = new Thread(() -> {
			try {
				start.await();
				for (int i = 0; i < THREAD_COUNT; i++) {
					repository.save(new Transaction(10, i, "root", null));
				}
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(exception);
			}
		});
		mutator.start();

		start.countDown();

		assertThat(workersDone.await(30, TimeUnit.SECONDS)).isTrue();
		mutator.join();

		assertThat(rejected.get()).isEqualTo(THREAD_COUNT);
		for (int i = 0; i < THREAD_COUNT; i++) {
			assertThat(repository.findById(1000L + i)).isEmpty();
		}
	}

	@Test
	void save_doesNotPersistCycleUnderConcurrentWrites() throws Exception {
		repository.save(new Transaction(10, 100, "a", null));
		repository.save(new Transaction(11, 100, "b", 10L));

		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch cycleAttemptsDone = new CountDownLatch(THREAD_COUNT);
		AtomicInteger rejected = new AtomicInteger();

		for (int i = 0; i < THREAD_COUNT; i++) {
			Thread worker = new Thread(() -> {
				try {
					start.await();
					repository.save(new Transaction(10, 100, "a", 11L));
				}
				catch (InvalidParentException exception) {
					rejected.incrementAndGet();
				}
				catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
				}
				finally {
					cycleAttemptsDone.countDown();
				}
			});
			worker.start();
		}

		Thread mutator = new Thread(() -> {
			try {
				start.await();
				for (int i = 0; i < THREAD_COUNT; i++) {
					repository.save(new Transaction(11, 100 + i, "b", 10L));
				}
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(exception);
			}
		});
		mutator.start();

		start.countDown();

		assertThat(cycleAttemptsDone.await(30, TimeUnit.SECONDS)).isTrue();
		mutator.join();

		assertThat(rejected.get()).isEqualTo(THREAD_COUNT);
		assertThat(repository.findById(10)).contains(new Transaction(10, 100, "a", null));
	}

}
