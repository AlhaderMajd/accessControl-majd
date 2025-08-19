package com.example.accesscontrol.config;

import org.springframework.stereotype.Component;

@Component
public class logs {
    public String mask(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] p = email.split("@", 2);
        return (p[0].isEmpty() ? "*" : p[0].substring(0, 1)) + "***@" + p[1];
    }
}
