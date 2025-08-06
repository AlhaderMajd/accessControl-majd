package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    public Optional<Group> findById(Long id) {
        return groupRepository.findById(id);
    }

    public Group getByIdOrThrow(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

}
