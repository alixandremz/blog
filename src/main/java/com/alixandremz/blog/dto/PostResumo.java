package com.alixandremz.blog.dto;

import java.time.Instant;
import java.util.List;

import com.alixandremz.blog.model.Post;

// versão enxuta do post para a listagem (sem o conteúdo nem os comentários)
public record PostResumo(
        Long id,
        String titulo,
        String autor,
        Instant criadoEm,
        List<String> tags,
        int totalComentarios
) {
    public static PostResumo de(Post post) {
        return new PostResumo(
                post.getId(),
                post.getTitulo(),
                post.getAutor(),
                post.getCriadoEm(),
                post.getTags().stream().map(t -> t.getNome()).sorted().toList(),
                post.getComentarios().size()
        );
    }
}
