package com.example.webhooksolver;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest req, byte[] body, ClientHttpRequestExecution exec) throws IOException {
        System.out.println("---- OUTBOUND REQUEST ----");
        System.out.println(req.getMethod() + " " + req.getURI());
        req.getHeaders().forEach((k, v) -> System.out.println(k + ": " + v));
        System.out.println("BODY: " + new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response = exec.execute(req, body);

        byte[] respBody;
        try (InputStream is = response.getBody()) {
            respBody = (is != null) ? StreamUtils.copyToByteArray(is) : new byte[0];
        } catch (Exception e) {
            System.err.println("Warning: failed to read response body in LoggingInterceptor: " + e.getMessage());
            respBody = new byte[0];
        }

        System.out.println("---- RESPONSE ----");
        try {
            System.out.println("Status: " + response.getStatusCode());
        } catch (Exception e) {
            System.out.println("Status: (unable to read status: " + e.getMessage() + ")");
        }
        response.getHeaders().forEach((k, v) -> System.out.println(k + ": " + v));
        System.out.println("RESP BODY: " + new String(respBody, StandardCharsets.UTF_8));

        // Return a wrapper exposing the buffered body so downstream code can read it again
        return new BufferingClientHttpResponse(response, respBody);
    }

    private static class BufferingClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse response;
        private final byte[] body;

        BufferingClientHttpResponse(ClientHttpResponse response, byte[] body) {
            this.response = response;
            this.body = body;
        }

        @Override public org.springframework.http.HttpStatusCode getStatusCode() throws IOException { return response.getStatusCode(); }
        @Override public int getRawStatusCode() throws IOException { return response.getRawStatusCode(); }
        @Override public String getStatusText() throws IOException { return response.getStatusText(); }
        @Override public void close() { response.close(); }
        @Override public org.springframework.http.HttpHeaders getHeaders() { return response.getHeaders(); }
        @Override public InputStream getBody() { return new java.io.ByteArrayInputStream(body); }
    }
}
