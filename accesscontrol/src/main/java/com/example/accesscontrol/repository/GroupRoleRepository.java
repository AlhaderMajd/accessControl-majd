package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.GroupRole;
import com.example.accesscontrol.entity.GroupRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface GroupRoleRepository extends JpaRepository<GroupRole, GroupRoleId> {
    void deleteAllByGroupIdInAndRoleIdIn(Set<Long> groupIds, Set<Long> roleIds);
    void deleteAllByRoleIdIn(Collection<Long> roleIds);

}
