package com.aleitox.transactions.domain;

import java.util.Map;

public final class TransactionHierarchyRules {

	private TransactionHierarchyRules() {
	}

	public static void validateParent(Map<Long, Transaction> transactions, long id, Long parentId) {
		if (parentId == null) {
			return;
		}
		if (!transactions.containsKey(parentId)) {
			throw new InvalidParentException("Parent does not exist: " + parentId);
		}
		if (wouldCreateCycle(transactions, id, parentId)) {
			throw new InvalidParentException("Cycle detected for transaction: " + id);
		}
	}

	private static boolean wouldCreateCycle(Map<Long, Transaction> transactions, long id, long parentId) {
		Long current = parentId;
		while (current != null) {
			if (current == id) {
				return true;
			}
			Transaction ancestor = transactions.get(current);
			if (ancestor == null) {
				return false;
			}
			current = ancestor.parentId();
		}
		return false;
	}

}
