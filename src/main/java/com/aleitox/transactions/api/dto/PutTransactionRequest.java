package com.aleitox.transactions.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PutTransactionRequest(
		@NotNull @PositiveOrZero Double amount,
		@NotBlank String type,
		@JsonProperty("parent_id") Long parentId) {
}
