package com.alixandremz.blog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.alixandremz.blog.repository.ComentarioRepository;
import com.alixandremz.blog.repository.PostRepository;
import com.alixandremz.blog.repository.TagRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BlogApiTests {

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostRepository posts;

    @Autowired
    private TagRepository tags;

    @Autowired
    private ComentarioRepository comentarios;

    @BeforeEach
    void limpar() {
        comentarios.deleteAll();
        posts.deleteAll();
        tags.deleteAll();
    }

    private long extrairId(String resposta) {
        Matcher matcher = ID.matcher(resposta);
        matcher.find();
        return Long.parseLong(matcher.group(1));
    }

    private long criarPost(String titulo, String tagsJson) throws Exception {
        String resposta = mvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + titulo + "\",\"conteudo\":\"conteudo do texto\","
                                + "\"autor\":\"Ana\",\"tags\":" + tagsJson + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extrairId(resposta);
    }

    private void comentar(long postId) throws Exception {
        mvc.perform(post("/posts/{id}/comentarios", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"João\",\"texto\":\"bom post\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void criaPostComTagsNovas() throws Exception {
        mvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Spring Boot\",\"conteudo\":\"texto\",\"autor\":\"Ana\","
                                + "\"tags\":[\"spring\",\"java\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.titulo").value("Spring Boot"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.tags[0]").value("java"))
                .andExpect(jsonPath("$.tags[1]").value("spring"))
                .andExpect(jsonPath("$.comentarios.length()").value(0));
    }

    @Test
    void reutilizaTagExistente() throws Exception {
        criarPost("Post A", "[\"java\"]");
        criarPost("Post B", "[\"java\"]");

        mvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("java"));
    }

    @Test
    void buscaPostInexistenteRetorna404() throws Exception {
        mvc.perform(get("/posts/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void listaPaginadaComTags() throws Exception {
        criarPost("Post A", "[\"java\"]");
        criarPost("Post B", "[\"python\"]");
        criarPost("Post C", "[\"java\"]");

        mvc.perform(get("/posts").param("tamanho", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.conteudo[0].tags").isArray());
    }

    @Test
    void filtraPostsPorTag() throws Exception {
        criarPost("Post A", "[\"java\"]");
        criarPost("Post B", "[\"python\"]");

        mvc.perform(get("/posts").param("tag", "JAVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("Post A"));
    }

    @Test
    void contaComentariosNaListagem() throws Exception {
        long postId = criarPost("Post A", "[]");
        comentar(postId);
        comentar(postId);

        mvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].totalComentarios").value(2));
    }

    @Test
    void adicionaComentario() throws Exception {
        long postId = criarPost("Post A", "[]");
        comentar(postId);

        mvc.perform(get("/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comentarios.length()").value(1))
                .andExpect(jsonPath("$.comentarios[0].autor").value("João"))
                .andExpect(jsonPath("$.comentarios[0].texto").value("bom post"));
    }

    @Test
    void removeComentario() throws Exception {
        long postId = criarPost("Post A", "[]");
        String resposta = mvc.perform(post("/posts/{id}/comentarios", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"João\",\"texto\":\"comentário\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long comentarioId = extrairId(resposta);

        mvc.perform(delete("/posts/{id}/comentarios/{cid}", postId, comentarioId))
                .andExpect(status().isNoContent());

        mvc.perform(get("/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comentarios.length()").value(0));
    }

    @Test
    void comentarPostInexistenteRetorna404() throws Exception {
        mvc.perform(post("/posts/9999/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"João\",\"texto\":\"oi\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tituloEmBrancoRetorna400() throws Exception {
        mvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"  \",\"conteudo\":\"x\",\"autor\":\"Ana\",\"tags\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.titulo").exists());
    }
}
