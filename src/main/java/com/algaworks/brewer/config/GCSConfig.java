package com.algaworks.brewer.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import jakarta.annotation.PreDestroy;

/**
 * Configuração do cliente GCP Cloud Storage para ambiente de produção GCP.
 *
 * Mirrors S3Config.java structure for consistency across multi-cloud deployments.
 *
 * Authentication methods supported (in order of preference):
 * 1. Environment variable: GOOGLE_APPLICATION_CREDENTIALS (path to JSON file)
 * 2. Environment variable: GCP_CREDENTIALS_JSON (inline JSON content)
 * 3. External property file: ~/.brewer-gcs.properties
 * 4. Application Default Credentials (ADC) - for GKE with Workload Identity
 *
 * Configuration properties:
 * - gcp.project-id: GCP project ID (required)
 * - gcp.credentials.location: Path to service account JSON (optional, fallback to env vars)
 * - gcp.credentials.json: Inline service account JSON (optional, fallback)
 */
@Profile("prod-gcp")
@Configuration
@PropertySource(value = { "file://${HOME}/.brewer-gcs.properties" }, ignoreResourceNotFound = true)
public class GCSConfig {

	private static final Logger logger = LoggerFactory.getLogger(GCSConfig.class);

	@Value("${gcp.project-id}")
	private String projectId;

	@Value("${gcp.credentials.location:#{null}}")
	private String credentialsLocation;

	@Value("${gcp.credentials.json:#{null}}")
	private String credentialsJson;

	private Storage storage;

	@Bean
	public Storage gcpStorage() {
		// SECURITY FIX: Validate project ID before creating client (fail-fast)
		if (projectId == null || projectId.trim().isEmpty()) {
			throw new IllegalStateException(
					"GCP_PROJECT_ID is required but not configured. " +
					"Set it as an environment variable or in .brewer-gcs.properties");
		}

		try {
			GoogleCredentials credentials = loadCredentials();

			// Build Storage client
			this.storage = StorageOptions.newBuilder()
					.setProjectId(projectId)
					.setCredentials(credentials)
					.build()
					.getService();

			logger.info("GCP Cloud Storage client initialized successfully for project: {}", projectId);
			return this.storage;

		} catch (IOException e) {
			throw new IllegalStateException("Failed to initialize GCP Cloud Storage client", e);
		}
	}

	/**
	 * Load Google credentials using multiple fallback strategies.
	 *
	 * Priority order:
	 * 1. Inline JSON from gcp.credentials.json property
	 * 2. Application Default Credentials (ADC) - automatically checks:
	 *    - GOOGLE_APPLICATION_CREDENTIALS environment variable (file path)
	 *    - gcloud config
	 *    - GKE Workload Identity
	 *    - Compute Engine metadata server
	 *
	 * @return GoogleCredentials instance
	 * @throws IOException if credentials cannot be loaded
	 */
	private GoogleCredentials loadCredentials() throws IOException {
		// Strategy 1: Inline JSON credentials (preferred for containers)
		if (credentialsJson != null && !credentialsJson.trim().isEmpty()) {
			logger.info("Loading GCP credentials from inline JSON (gcp.credentials.json)");
			ByteArrayInputStream credStream = new ByteArrayInputStream(
					credentialsJson.getBytes(StandardCharsets.UTF_8));
			return ServiceAccountCredentials.fromStream(credStream);
		}

		// Strategy 2: Application Default Credentials (ADC)
		// This automatically checks GOOGLE_APPLICATION_CREDENTIALS env var,
		// then gcloud config, then GKE Workload Identity, etc.
		logger.info("Loading GCP credentials using Application Default Credentials (ADC)");
		return GoogleCredentials.getApplicationDefault();
	}

	/**
	 * Cleanup method to properly close Storage client and release resources.
	 *
	 * This method is called by Spring when the bean is destroyed.
	 * Mirrors S3Config cleanup pattern for consistency.
	 *
	 * NOTE: The Storage interface in google-cloud-storage doesn't directly implement
	 * Closeable, but the underlying implementation (StorageImpl) does have cleanup logic.
	 * We attempt to call close() if available via reflection for forward compatibility.
	 */
	@PreDestroy
	public void cleanup() {
		if (storage != null) {
			try {
				logger.info("Closing GCP Storage client and releasing resources");

				// Attempt to close if the implementation supports it
				if (storage instanceof AutoCloseable) {
					((AutoCloseable) storage).close();
					logger.info("GCP Storage client closed successfully");
				} else {
					logger.debug("GCP Storage client doesn't implement AutoCloseable, skipping explicit close");
				}
			} catch (Exception e) {
				logger.error("Error closing GCP Storage client", e);
			}
		}
	}

}