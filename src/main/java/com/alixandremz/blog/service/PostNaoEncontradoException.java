package com.alixandremz.blog.service;

public class PostNaoEncontradoException extends RuntimeException {

    public PostNaoEncontradoException(Long id) {
        super("post não encontrado: id " + id);
    }
}
