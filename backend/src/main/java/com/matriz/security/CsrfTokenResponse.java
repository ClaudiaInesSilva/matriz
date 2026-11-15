package com.matriz.security;

public record CsrfTokenResponse(String token, String headerName) {}
