package com.alixandremz.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alixandremz.blog.model.Comentario;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
}
