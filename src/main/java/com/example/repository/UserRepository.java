package com.example.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.UserSecurity;

public interface    UserRepository extends JpaRepository<UserSecurity, Long>  {
    Optional<UserSecurity> findByUserId(String userId);
}
