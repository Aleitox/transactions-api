package com.aleitox.transactions.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aleitox.transactions.api.dto.PutTransactionRequest;
import com.aleitox.transactions.api.dto.StatusResponse;
import com.aleitox.transactions.api.dto.SumResponse;
import com.aleitox.transactions.application.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PutMapping("/{id}")
	public StatusResponse put(@PathVariable long id, @Valid @RequestBody PutTransactionRequest request) {
		String normalizedType = TransactionTypeNormalizer.normalize(request.type());
		transactionService.save(id, request.amount(), normalizedType, request.parentId());
		return new StatusResponse("ok");
	}

	@GetMapping("/types/{type}")
	public List<Long> getTypesByType(@PathVariable String type) {
		String normalizedType = TransactionTypeNormalizer.normalize(type);
		return transactionService.findIdsByType(normalizedType);
	}

	@GetMapping("/sum/{id}")
	public SumResponse getSum(@PathVariable long id) {
		return new SumResponse(transactionService.sumTransitive(id));
	}

}
