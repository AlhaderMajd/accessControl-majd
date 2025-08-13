package com.example.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_groups",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_groups_user_group",
                columnNames = {"user_id", "group_id"}
        ),
        indexes = {
                @Index(name = "idx_user_groups_user", columnList = "user_id"),
                @Index(name = "idx_user_groups_group", columnList = "group_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "group"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_groups_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_groups_group"))
    private Group group;

    @Version
    private long version;
}
