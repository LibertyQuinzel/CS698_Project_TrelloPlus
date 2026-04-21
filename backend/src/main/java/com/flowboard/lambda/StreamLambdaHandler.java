package com.flowboard.lambda;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.flowboard.FlowBoardApplication;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamLambdaHandler implements RequestStreamHandler {
    private static final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            // Fetch secrets from AWS Secrets Manager before initializing Spring
            loadSecretsFromSecretsManager();
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(FlowBoardApplication.class);
        } catch (ContainerInitializationException e) {
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        } catch (Exception e) {
            throw new RuntimeException("Could not load secrets or initialize Spring Boot application", e);
        }
    }

    /**
     * Loads secrets from AWS Secrets Manager and sets them as system properties
     * for Spring Boot to use.
     */
    private static void loadSecretsFromSecretsManager() {
        try (SecretsManagerClient client = SecretsManagerClient.builder().build()) {
            // Load JWT_SECRET
            String jwtSecret = getSecretValue(client, "flowboard/jwt-secret");
            if (jwtSecret != null && !jwtSecret.isEmpty()) {
                System.setProperty("JWT_SECRET", jwtSecret);
            }

            // Load DB_URL
            String dbUrl = getSecretValue(client, "flowboard/db-url");
            if (dbUrl != null && !dbUrl.isEmpty()) {
                System.setProperty("DB_URL", dbUrl);
            }

            // Load DB_PASSWORD
            String dbPassword = getSecretValue(client, "flowboard/db-password");
            if (dbPassword != null && !dbPassword.isEmpty()) {
                System.setProperty("DB_PASSWORD", dbPassword);
            }

            System.out.println("Successfully loaded secrets from AWS Secrets Manager");
        } catch (Exception e) {
            System.err.println("Warning: Could not load all secrets from Secrets Manager: " + e.getMessage());
            // Continue anyway - can fall back to environment variables
        }
    }

    /**
     * Retrieves a single secret value from AWS Secrets Manager.
     */
    private static String getSecretValue(SecretsManagerClient client, String secretName) {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
            GetSecretValueResponse response = client.getSecretValue(request);
            return response.secretString();
        } catch (Exception e) {
            System.err.println("Could not retrieve secret " + secretName + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
        throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}