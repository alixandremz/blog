package com.alixandremz.blog.service;

public class ComentarioNaoEncontradoException extends RuntimeException {

    public ComentarioNaoEncontradoException(Long id) {
        super("comentário não encontrado: id " + id);
    }
}
