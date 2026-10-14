package com.alixandremz.blog.web;

import java.net.URI;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alixandremz.blog.dto.ComentarioRequest;
import com.alixandremz.blog.dto.ComentarioResponse;
import com.alixandremz.blog.dto.PaginaResponse;
import com.alixandremz.blog.dto.PostDetalhe;
import com.alixandremz.blog.dto.PostRequest;
import com.alixandremz.blog.dto.PostResumo;
import com.alixandremz.blog.service.PostService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/posts")
@Validated
public class PostController {

    private final PostService servico;

    public PostController(PostService servico) {
        this.servico = servico;
    }

    @GetMapping
    public PaginaResponse<PostResumo> listar(
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "a página não pode ser negativa") int pagina,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "o tamanho mínimo é 1")
            @Max(value = 100, message = "o tamanho máximo é 100") int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm"));
        return servico.listar(tag, pageable);
    }

    @GetMapping("/{id}")
    public PostDetalhe buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @PostMapping
    public ResponseEntity<PostDetalhe> criar(@Valid @RequestBody PostRequest pedido) {
        PostDetalhe criado = servico.criar(pedido);
        return ResponseEntity.created(URI.create("/posts/" + criado.id())).body(criado);
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(@PathVariable Long id,
            @Valid @RequestBody ComentarioRequest pedido) {
        ComentarioResponse criado = servico.comentar(id, pedido);
        return ResponseEntity.created(URI.create("/posts/" + id + "/comentarios/" + criado.id())).body(criado);
    }

    @DeleteMapping("/{id}/comentarios/{comentarioId}")
    public ResponseEntity<Void> removerComentario(@PathVariable Long id, @PathVariable Long comentarioId) {
        servico.removerComentario(id, comentarioId);
        return ResponseEntity.noContent().build();
    }
}
