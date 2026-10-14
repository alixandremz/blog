package com.alixandremz.blog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.alixandremz.blog.model.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByTagsNomeIgnoreCase(String nome, Pageable pageable);
}
