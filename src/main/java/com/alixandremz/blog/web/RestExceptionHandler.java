package com.alixandremz.blog.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.alixandremz.blog.service.ComentarioNaoEncontradoException;
import com.alixandremz.blog.service.PostNaoEncontradoException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler({PostNaoEncontradoException.class, ComentarioNaoEncontradoException.class})
    public ResponseEntity<ErroResposta> naoEncontrado(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErroResposta.de(ex.getMessage()));
    }

    // ex.: nome de tag duplicado (constraint unique)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> dadosEmConflito(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResposta.de("registro já existe ou viola uma restrição do banco"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> invalido(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.put(erro.getField(), erro.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroResposta("dados inválidos", campos));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResposta> parametroInvalido(ConstraintViolationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(erro -> campos.put(erro.getPropertyPath().toString(), erro.getMessage()));
        return ResponseEntity.badRequest().body(new ErroResposta("dados inválidos", campos));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResposta> parametroInvalido(HandlerMethodValidationException ex) {
        return ResponseEntity.badRequest().body(ErroResposta.de("parâmetros da requisição inválidos"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ErroResposta.de("corpo da requisição inválido"));
    }
}
