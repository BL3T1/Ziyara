package com.ziyara.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.core.TestcontainersConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies Kafka bootstrap and basic publish/consume works
 * end-to-end using a real Kafka container (Testcontainers).
 *
 * Also verifies that the application starts correctly with Kafka enabled
 * and that authenticated REST calls succeed when Kafka is in the context.
 *
 * Uses the functest profile (permissive security) so CSRF + auth are bypassed
 * in the same way as EndpointFunctionalTest.
 *
 * Run with: ./gradlew test -PrunDockerTests
 */
@Tag("docker")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("functest")
@Import({TestcontainersConfiguration.class, KafkaIntegrationTest.FunctestSecurityConfig.class})
@TestPropertySource(properties = {
    "ziyara.notifications.kafka.enabled=true",
    "ziyara.notifications.kafka.topic-staff=ziyara.notifications.staff.test",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.group-id=test-consumer-group"
})
@DirtiesContext
class KafkaIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class FunctestSecurityConfig {
        @Bean(name = "kafkaTestAuthChain")
        @Order(1)
        SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/auth/**", "/actuator/**")
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean(name = "kafkaTestMainChain")
        @Order(2)
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/**")
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean(name = "kafkaTestPasswordEncoder")
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void kafkaTemplate_publishAndConsume_messageReceived() throws Exception {
        String testTopic = "ziyara.notifications.staff.test";
        String testPayload = "{\"test\":\"kafka-integration\",\"ts\":" + System.currentTimeMillis() + "}";

        // Publish a message directly via KafkaTemplate
        kafkaTemplate.send(testTopic, "test-key", testPayload).get();

        // Consume with a raw KafkaConsumer to verify delivery
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-verify-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(testTopic));
            long deadline = System.currentTimeMillis() + 10_000;
            boolean found = false;
            while (System.currentTimeMillis() < deadline && !found) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (testPayload.equals(record.value())) {
                        found = true;
                        break;
                    }
                }
            }
            assertThat(found).as("Expected message to be delivered to Kafka topic within 10 seconds").isTrue();
        }
    }

    @Test
    void applicationStartsWithKafkaEnabled_authenticatedRequestSucceeds() throws Exception {
        String baseUrl = "http://localhost:" + port + "/api/v1";
        String token = registerAndLogin(baseUrl);
        assertThat(token).isNotBlank();

        // Verify an authenticated call succeeds (this exercises the DB + security path
        // while Kafka is enabled — confirms no startup/wiring failure)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/users/me", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void notificationsEndpoint_returnsOk_withKafkaEnabled() throws Exception {
        String baseUrl = "http://localhost:" + port + "/api/v1";
        String token = registerAndLogin(baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/notifications", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private String registerAndLogin(String baseUrl) throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "kafka-test-" + suffix + "@ziyarah.com";
        String password = "Test123!";

        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        String registerBody = "{\"email\":\"%s\",\"password\":\"%s\",\"role\":\"CUSTOMER\"}".formatted(email, password);
        restTemplate.postForEntity(baseUrl + "/auth/register",
                new HttpEntity<>(registerBody, json), String.class);

        String loginBody = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        ResponseEntity<String> loginResp = restTemplate.postForEntity(baseUrl + "/auth/login",
                new HttpEntity<>(loginBody, json), String.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<?, ?> parsed = objectMapper.readValue(loginResp.getBody(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) parsed.get("data");
        return (String) data.get("accessToken");
    }
}
