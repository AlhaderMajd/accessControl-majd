package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    @Query("SELECT r.name FROM Role r WHERE r.name IN :names")
    List<String> findExistingNames(@Param("names") List<String> names);
    Page<Role> findByNameContainingIgnoreCase(String name, Pageable pageable);
}