package com.alixandremz.blog.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank(message = "o título é obrigatório")
        @Size(max = 180, message = "o título deve ter no máximo 180 caracteres")
        String titulo,

        @NotBlank(message = "o conteúdo é obrigatório")
        @Size(max = 4000, message = "o conteúdo deve ter no máximo 4000 caracteres")
        String conteudo,

        @NotBlank(message = "o autor é obrigatório")
        @Size(max = 120, message = "o autor deve ter no máximo 120 caracteres")
        String autor,

        // nomes de tags; as que não existirem são criadas na hora
        @Size(max = 10, message = "no máximo 10 tags por post")
        Set<@NotBlank(message = "tag não pode ser vazia")
            @Size(max = 30, message = "cada tag deve ter no máximo 30 caracteres") String> tags
) {
}
