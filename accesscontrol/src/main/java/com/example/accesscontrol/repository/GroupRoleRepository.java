package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.GroupRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface GroupRoleRepository extends JpaRepository<GroupRole, GroupRole.Id> {
    void deleteAllByIdGroupIdInAndIdRoleIdIn(Set<Long> groupIds, Set<Long> roleIds);
    void deleteAllByIdRoleIdIn(Collection<Long> roleIds);
    boolean existsByIdGroupIdAndIdRoleId(Long groupId, Long roleId);
    List<GroupRole> findByIdGroupIdInAndIdRoleIdIn(Collection<Long> groupIds, Collection<Long> roleIds);
    List<GroupRole> findByIdGroupId(Long groupId);
    void deleteAllByIdGroupIdIn(java.util.Collection<Long> groupIds);

}
