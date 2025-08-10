package com.example.redpandaapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.redpandaapp.model.DiaryPost;

public interface DiaryPostRepository extends JpaRepository<DiaryPost, Long> {}
