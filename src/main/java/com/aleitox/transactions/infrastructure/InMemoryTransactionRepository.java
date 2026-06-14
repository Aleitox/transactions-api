package com.aleitox.transactions.infrastructure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Repository;

import com.aleitox.transactions.domain.Transaction;
import com.aleitox.transactions.domain.TransactionHierarchyRules;
import com.aleitox.transactions.domain.TransactionRepository;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

	private final Map<Long, Transaction> transactions = new HashMap<>();
	private final Map<String, Set<Long>> byType = new HashMap<>();
	private final Map<Long, List<Long>> childrenByParent = new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	@Override
	public void save(Transaction transaction) {
		lock.writeLock().lock();
		try {
			TransactionHierarchyRules.validateParent(
					transactions, transaction.id(), transaction.parentId());

			Transaction previous = transactions.get(transaction.id());

			if (previous != null) {
				if (!previous.type().equals(transaction.type())) {
					removeFromTypeIndex(previous.type(), transaction.id());
					addToTypeIndex(transaction.type(), transaction.id());
				}
				if (!Objects.equals(previous.parentId(), transaction.parentId())) {
					if (previous.parentId() != null) {
						removeFromChildrenIndex(previous.parentId(), transaction.id());
					}
					if (transaction.parentId() != null) {
						addToChildrenIndex(transaction.parentId(), transaction.id());
					}
				}
			} else {
				addToTypeIndex(transaction.type(), transaction.id());
				if (transaction.parentId() != null) {
					addToChildrenIndex(transaction.parentId(), transaction.id());
				}
			}

			transactions.put(transaction.id(), transaction);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Optional<Transaction> findById(long id) {
		lock.readLock().lock();
		try {
			return Optional.ofNullable(transactions.get(id));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean exists(long id) {
		lock.readLock().lock();
		try {
			return transactions.containsKey(id);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<Long> findIdsByType(String type) {
		lock.readLock().lock();
		try {
			Set<Long> ids = byType.get(type);
			if (ids == null || ids.isEmpty()) {
				return List.of();
			}
			return List.copyOf(ids);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<Long> findChildrenIds(long parentId) {
		lock.readLock().lock();
		try {
			List<Long> children = childrenByParent.get(parentId);
			if (children == null || children.isEmpty()) {
				return List.of();
			}
			return List.copyOf(children);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Optional<Double> sumTransitive(long transactionId) {
		lock.readLock().lock();
		try {
			if (!transactions.containsKey(transactionId)) {
				return Optional.empty();
			}

			double sum = 0;
			ArrayDeque<Long> queue = new ArrayDeque<>();
			queue.add(transactionId);

			while (!queue.isEmpty()) {
				long id = queue.removeFirst();
				Transaction transaction = transactions.get(id);
				sum += transaction.amount();

				List<Long> children = childrenByParent.get(id);
				if (children != null) {
					queue.addAll(children);
				}
			}

			return Optional.of(sum);
		} finally {
			lock.readLock().unlock();
		}
	}

	private void addToTypeIndex(String type, long id) {
		byType.computeIfAbsent(type, ignored -> new HashSet<>()).add(id);
	}

	private void removeFromTypeIndex(String type, long id) {
		Set<Long> ids = byType.get(type);
		if (ids == null) {
			return;
		}
		ids.remove(id);
		if (ids.isEmpty()) {
			byType.remove(type);
		}
	}

	private void addToChildrenIndex(long parentId, long childId) {
		childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(childId);
	}

	private void removeFromChildrenIndex(long parentId, long childId) {
		List<Long> children = childrenByParent.get(parentId);
		if (children == null) {
			return;
		}
		children.remove(childId);
		if (children.isEmpty()) {
			childrenByParent.remove(parentId);
		}
	}

}
