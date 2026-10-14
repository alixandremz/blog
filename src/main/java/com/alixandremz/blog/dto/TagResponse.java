package com.alixandremz.blog.dto;

import com.alixandremz.blog.model.Tag;

public record TagResponse(Long id, String nome) {

    public static TagResponse de(Tag tag) {
        return new TagResponse(tag.getId(), tag.getNome());
    }
}
