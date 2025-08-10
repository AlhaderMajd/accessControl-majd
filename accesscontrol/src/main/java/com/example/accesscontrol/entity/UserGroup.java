package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "user_groups", uniqueConstraints = @UniqueConstraint(name = "uk_user_groups_user_group", columnNames = {"user_id", "group_id"}),
        indexes = {
                @Index(name = "idx_user_groups_user", columnList = "user_id"),
                @Index(name = "idx_user_groups_group", columnList = "group_id")
        })
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserGroup {

    @EmbeddedId
    private Id id;

    @Embeddable
    @Data
    @NoArgsConstructor @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Column(name = "group_id", nullable = false)
        private Long groupId;
    }
}