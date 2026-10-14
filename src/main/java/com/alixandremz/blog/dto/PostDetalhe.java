package com.alixandremz.blog.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.alixandremz.blog.model.Post;

// versão completa do post, com conteúdo e comentários
public record PostDetalhe(
        Long id,
        String titulo,
        String conteudo,
        String autor,
        Instant criadoEm,
        Instant atualizadoEm,
        List<String> tags,
        List<ComentarioResponse> comentarios
) {
    public static PostDetalhe de(Post post) {
        return new PostDetalhe(
                post.getId(),
                post.getTitulo(),
                post.getConteudo(),
                post.getAutor(),
                post.getCriadoEm(),
                post.getAtualizadoEm(),
                post.getTags().stream().map(t -> t.getNome()).sorted().toList(),
                post.getComentarios().stream()
                        .sorted(Comparator.comparing(c -> c.getCriadoEm()))
                        .map(ComentarioResponse::de)
                        .toList()
        );
    }
}
