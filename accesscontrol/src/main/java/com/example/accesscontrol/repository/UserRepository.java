package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.User;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findAllByEmailIn(List<String> emails);
}
