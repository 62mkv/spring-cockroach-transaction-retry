package com.example.springcockroachtransactionretry.repository;

import com.example.springcockroachtransactionretry.data.Entity2;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface Entity2Repository extends R2dbcRepository<Entity2, UUID> {

    Mono<Long> deleteAllByParentId(UUID parentId);
}
