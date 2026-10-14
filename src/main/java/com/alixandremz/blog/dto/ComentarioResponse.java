package com.alixandremz.blog.dto;

import java.time.Instant;

import com.alixandremz.blog.model.Comentario;

public record ComentarioResponse(Long id, String autor, String texto, Instant criadoEm) {

    public static ComentarioResponse de(Comentario comentario) {
        return new ComentarioResponse(
                comentario.getId(),
                comentario.getAutor(),
                comentario.getTexto(),
                comentario.getCriadoEm()
        );
    }
}
