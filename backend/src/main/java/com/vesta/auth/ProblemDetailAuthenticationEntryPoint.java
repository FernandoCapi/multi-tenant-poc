package com.vesta.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 401 for missing, expired, tampered or otherwise invalid tokens, as a problem detail in
 * Portuguese. The body never says why the token was rejected.
 */
class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String BODY = """
            {"type":"about:blank","title":"Não autenticado","status":401,\
            "detail":"Faça login para acessar este recurso."}""";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, TokenService.TOKEN_TYPE);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(BODY);
    }
}
