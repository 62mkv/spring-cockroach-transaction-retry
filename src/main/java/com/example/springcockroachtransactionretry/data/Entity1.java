package com.example.springcockroachtransactionretry.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table
public class Entity1 {
    @Id
    UUID id;

    String name;
}
