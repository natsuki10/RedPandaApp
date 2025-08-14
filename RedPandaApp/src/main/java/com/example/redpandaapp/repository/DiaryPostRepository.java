package com.example.redpandaapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.redpandaapp.model.DiaryPost;


public interface DiaryPostRepository extends JpaRepository<DiaryPost, Long> {
	Page<DiaryPost> findByPandaName(String pandaName, Pageable pageable);
	Page<DiaryPost> findByPandaNameContainingIgnoreCase(String q, Pageable pageable);
}