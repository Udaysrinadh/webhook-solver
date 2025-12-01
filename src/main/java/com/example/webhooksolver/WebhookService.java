package com.example.webhooksolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.io.IOException;
import java.util.Collections;

@Component
public class WebhookService {

    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String GENERATE_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    public WebhookService() {
        // Build Apache HttpClient and RestTemplate (already used and working in your setup)
        CloseableHttpClient httpClient = HttpClients.custom()
                .disableCookieManagement()
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate r = new RestTemplate(factory);

        // Logging interceptor (leave as previously added)
        r.setInterceptors(Collections.singletonList(new LoggingInterceptor()));

        // Allow reading 4xx/5xx bodies without throwing
        r.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });

        this.rest = r;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            System.out.println("Sending generateWebhook request to: " + GENERATE_URL);

            String reqBody = "{\"name\":\"John Doe\",\"regNo\":\"REG12346\",\"email\":\"john@example.com\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(reqBody, headers);

            ResponseEntity<String> resp = rest.postForEntity(GENERATE_URL, entity, String.class);
            System.out.println("Raw webhook response: " + resp.getBody());

            JsonNode json = mapper.readTree(resp.getBody());
            String webhook = json.get("webhook").asText();
            String accessToken = json.get("accessToken").asText();

            System.out.println("Received webhook: " + webhook);
            System.out.println("Received accessToken: " + accessToken);

            // Final SQL (kept readable here, will be sanitized)
            String finalQuery = "SELECT d.department_name AS DEPARTMENT_NAME, " +
                    "ROUND(AVG(EXTRACT(year FROM AGE(CURRENT_DATE, e.dob)))::numeric,2) AS AVERAGE_AGE, " +
                    "COALESCE(array_to_string((SELECT array_agg(n ORDER BY n) FROM (" +
                    "SELECT (e2.first_name || ' ' || e2.last_name) AS n " +
                    "FROM employee e2 JOIN payments p2 ON p2.emp_id = e2.emp_id " +
                    "WHERE p2.amount > 70000 AND e2.department = d.department_id " +
                    "GROUP BY e2.emp_id, e2.first_name, e2.last_name ORDER BY e2.emp_id LIMIT 10) sub), ', '), '') AS EMPLOYEE_LIST " +
                    "FROM department d JOIN employee e ON e.department = d.department_id " +
                    "JOIN payments p ON p.emp_id = e.emp_id AND p.amount > 70000 " +
                    "GROUP BY d.department_id, d.department_name ORDER BY d.department_id DESC;";

            // send using known-good header format: Authorization: <token>
            sendWithPlainAuthorization(webhook, finalQuery, accessToken);

        } catch (Exception ex) {
            System.err.println("Startup error:");
            ex.printStackTrace();
        }
    }

    private void sendWithPlainAuthorization(String webhook, String finalQuery, String accessToken) {
        // sanitize: collapse whitespace and escape double quotes
        String sanitized = finalQuery.replaceAll("\\s+", " ").trim();
        String escaped = sanitized.replace("\"", "\\\"");
        String submitBody = String.format("{\"finalQuery\":\"%s\"}", escaped);

        System.out.println("Posting sanitized SQL to: " + webhook);
        System.out.println("Sanitized SQL length: " + sanitized.length());

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        // **Important:** server expects the raw token in the Authorization header (no 'Bearer' prefix)
        h.set("Authorization", accessToken);

        HttpEntity<String> req = new HttpEntity<>(submitBody, h);
        ResponseEntity<String> resp = rest.postForEntity(webhook, req, String.class);

        System.out.println("Submission response status: " + resp.getStatusCode());
        System.out.println("Submission response body: " + resp.getBody());
    }
}
