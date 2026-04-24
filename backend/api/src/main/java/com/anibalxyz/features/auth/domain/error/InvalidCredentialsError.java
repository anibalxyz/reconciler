package com.anibalxyz.features.auth.domain.error;

// Using record to avoid declaring boilerplate methods. It might change in the future.
public record InvalidCredentialsError() implements AuthDomainError {}
