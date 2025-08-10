package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "`groups`", uniqueConstraints = @UniqueConstraint(name = "uk_groups_name", columnNames = "name"))
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
}
