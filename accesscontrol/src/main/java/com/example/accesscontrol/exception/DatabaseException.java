package com.example.accesscontrol.exception;

public class DatabaseException extends Exception{
    public DatabaseException() {
        super("Database connection failed. Please try again later.");
    }
}
