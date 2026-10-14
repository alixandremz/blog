package com.alixandremz.blog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alixandremz.blog.model.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNomeIgnoreCase(String nome);

    List<Tag> findAllByOrderByNomeAsc();
}
