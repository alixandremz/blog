package com.alixandremz.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioRequest(
        @NotBlank(message = "o autor é obrigatório")
        @Size(max = 120, message = "o autor deve ter no máximo 120 caracteres")
        String autor,

        @NotBlank(message = "o comentário não pode ser vazio")
        @Size(max = 1000, message = "o comentário deve ter no máximo 1000 caracteres")
        String texto
) {
}
