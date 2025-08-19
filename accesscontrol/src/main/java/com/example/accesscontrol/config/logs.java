package com.example.accesscontrol.config;

import org.springframework.stereotype.Component;

@Component
public class logs {
    public String mask(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) return "unknown";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String head = local.isEmpty() ? "*" : local.substring(0, 1);
        return head + "***@" + domain;
    }
}
