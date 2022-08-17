package com.example.springcockroachtransactionretry;

import com.example.springcockroachtransactionretry.data.Entity1;
import com.example.springcockroachtransactionretry.data.Entity2;
import com.example.springcockroachtransactionretry.repository.Entity1Repository;
import com.example.springcockroachtransactionretry.repository.Entity2Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
@Testcontainers
class SpringCockroachTransactionRetryApplicationTests {

	private static final Logger log = LoggerFactory.getLogger("main");
	private static final DockerImageName cockroachImage = DockerImageName.parse("cockroachdb/cockroach:v22.1.5");

	@Container
	private static GenericContainer<?> cockroach = new GenericContainer<>(cockroachImage)
			.withExposedPorts(26257)
			.withLogConsumer(new Slf4jLogConsumer(log))
			.withCommand("start-single-node --insecure");

	@DynamicPropertySource
	static void exposeContainerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://root@localhost:%d/defaultdb", cockroach.getMappedPort(26257)));
		registry.add("spring.liquibase.url", () -> String.format("jdbc:postgresql://localhost:%d/defaultdb", cockroach.getMappedPort(26257)));
	}

	@Autowired
	private Entity1Repository parentRepository;

	@Autowired
	private Entity2Repository childRepository;

	@Autowired
	private ReactiveTransactionManager transactionManager;
	private TransactionalOperator rxtx;

	@BeforeEach
	void setUp() {
		this.rxtx = TransactionalOperator.create(this.transactionManager);
	}

	@Test
	void testTransactionRetries() {
		var parent1 = Entity1.builder().name("name1").build();
		var child1 =  Entity2.builder().name("child1").build();

		var parent2 = Entity1.builder().name("name2").build();
		var child2 =  Entity2.builder().name("child2").build();
		var victim = rxtx.transactional(
				insertWithChildren(parent1, child1)
						.delayUntil(ignore -> childRepository.findAll().then())
						.thenMany(parentRepository.findAll())
						.delaySequence(Duration.ofSeconds(3))
						.flatMap(e -> updateWithChildren(e, child1))
		).log("victim");

		var conflicting = rxtx.transactional(
				insertWithChildren(parent2, child2)
		).log("conflicting");

		var flow = Flux.merge(victim, conflicting);

		StepVerifier.create(flow)
				.expectNextCount(2)
				.verifyError();
	}

	private Mono<Entity1> updateWithChildren(Entity1 entity1, Entity2... children) {
		assert entity1.getId() != null;
		return parentRepository.save(entity1)
				.delayUntil(e -> childRepository.deleteAllByParentId(e.getId()))
				.delayUntil(e -> childRepository.saveAll(Stream.of(children).peek(c -> {
					c.setParentId(e.getId());
					c.setId(null);
				}).collect(Collectors.toList())));
	}

	private Mono<Entity1> insertWithChildren(Entity1 entity1, Entity2... children) {
		assert entity1.getId() == null;
		return parentRepository.save(entity1)
				.delayUntil(e -> childRepository.saveAll(Stream.of(children).peek(c -> {
					c.setParentId(e.getId());
					c.setId(null);
				}).collect(Collectors.toList())));
	}

}
