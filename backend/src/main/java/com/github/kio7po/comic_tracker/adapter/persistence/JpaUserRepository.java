package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.port.persistence.UserRepository;

public interface JpaUserRepository extends JpaRepository<User, Long>, UserRepository {
}
