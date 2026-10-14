package com.alixandremz.blog.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alixandremz.blog.dto.TagResponse;
import com.alixandremz.blog.service.PostService;

@RestController
@RequestMapping("/tags")
public class TagController {

    private final PostService servico;

    public TagController(PostService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<TagResponse> listar() {
        return servico.listarTags();
    }
}
