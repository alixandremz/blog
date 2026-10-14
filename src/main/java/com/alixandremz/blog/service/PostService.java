package com.alixandremz.blog.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alixandremz.blog.dto.ComentarioRequest;
import com.alixandremz.blog.dto.ComentarioResponse;
import com.alixandremz.blog.dto.PaginaResponse;
import com.alixandremz.blog.dto.PostDetalhe;
import com.alixandremz.blog.dto.PostRequest;
import com.alixandremz.blog.dto.PostResumo;
import com.alixandremz.blog.dto.TagResponse;
import com.alixandremz.blog.model.Comentario;
import com.alixandremz.blog.model.Post;
import com.alixandremz.blog.model.Tag;
import com.alixandremz.blog.repository.ComentarioRepository;
import com.alixandremz.blog.repository.PostRepository;
import com.alixandremz.blog.repository.TagRepository;

@Service
public class PostService {

    private final PostRepository postRepositorio;
    private final TagRepository tagRepositorio;
    private final ComentarioRepository comentarioRepositorio;

    public PostService(PostRepository postRepositorio, TagRepository tagRepositorio,
            ComentarioRepository comentarioRepositorio) {
        this.postRepositorio = postRepositorio;
        this.tagRepositorio = tagRepositorio;
        this.comentarioRepositorio = comentarioRepositorio;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PostResumo> listar(String tag, Pageable pageable) {
        Page<Post> pagina = (tag == null || tag.isBlank())
                ? postRepositorio.findAll(pageable)
                : postRepositorio.findByTagsNomeIgnoreCase(tag.trim(), pageable);
        return PaginaResponse.de(pagina.map(PostResumo::de));
    }

    @Transactional(readOnly = true)
    public PostDetalhe buscar(Long id) {
        return PostDetalhe.de(obter(id));
    }

    @Transactional
    public PostDetalhe criar(PostRequest pedido) {
        Post post = new Post();
        post.setTitulo(pedido.titulo().trim());
        post.setConteudo(pedido.conteudo().trim());
        post.setAutor(pedido.autor().trim());
        post.setTags(resolverTags(pedido.tags()));
        return PostDetalhe.de(postRepositorio.save(post));
    }

    @Transactional
    public ComentarioResponse comentar(Long postId, ComentarioRequest pedido) {
        Post post = obter(postId);
        Comentario comentario = new Comentario();
        comentario.setPost(post);
        comentario.setAutor(pedido.autor().trim());
        comentario.setTexto(pedido.texto().trim());
        return ComentarioResponse.de(comentarioRepositorio.save(comentario));
    }

    @Transactional
    public void removerComentario(Long postId, Long comentarioId) {
        Post post = obter(postId);
        Comentario comentario = comentarioRepositorio.findById(comentarioId)
                .orElseThrow(() -> new ComentarioNaoEncontradoException(comentarioId));
        if (!comentario.getPost().getId().equals(postId)) {
            throw new ComentarioNaoEncontradoException(comentarioId);
        }
        // remove da coleção do post: o orphanRemoval apaga a linha do banco
        post.getComentarios().remove(comentario);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listarTags() {
        return tagRepositorio.findAllByOrderByNomeAsc().stream().map(TagResponse::de).toList();
    }

    // reaproveita tags existentes e cria as que faltam
    private Set<Tag> resolverTags(Set<String> nomes) {
        Set<Tag> tags = new LinkedHashSet<>();
        if (nomes == null) {
            return tags;
        }
        for (String nome : nomes) {
            String limpo = nome.trim();
            if (limpo.isEmpty()) {
                continue;
            }
            Tag tag = tagRepositorio.findByNomeIgnoreCase(limpo)
                    .orElseGet(() -> {
                        Tag nova = new Tag();
                        nova.setNome(limpo);
                        return tagRepositorio.save(nova);
                    });
            tags.add(tag);
        }
        return tags;
    }

    private Post obter(Long id) {
        return postRepositorio.findById(id).orElseThrow(() -> new PostNaoEncontradoException(id));
    }
}
