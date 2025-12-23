package com.algaworks.brewer.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Configuração do cliente AWS S3 para ambiente de produção.
 *
 * MIGRATION: Phase 14 - Migrated from AWS SDK v1 to v2
 *
 * Changes:
 * - BasicAWSCredentials → AwsBasicCredentials
 * - AWSStaticCredentialsProvider → StaticCredentialsProvider
 * - AmazonS3ClientBuilder → S3Client.builder()
 * - Region String → Region enum
 * - Added explicit HTTP client configuration
 * - Added @PreDestroy for proper resource cleanup
 *
 * Benefits of v2:
 * - Better performance and lower memory footprint
 * - Modern, fluent API design
 * - Better async support (can upgrade to S3AsyncClient later)
 * - Active development and support
 */
@Profile("prod")
@Configuration
@PropertySource(value = { "file://${HOME}/.brewer-s3.properties" }, ignoreResourceNotFound = true)
public class S3Config {

	private static final Logger logger = LoggerFactory.getLogger(S3Config.class);

	@Value("${AWS_ACCESS_KEY_ID}")
	private String accessKeyId;

	@Value("${AWS_SECRET_ACCESS_KEY}")
	private String secretAccessKey;

	@Value("${AWS_REGION:sa-east-1}")
	private String region;

	private S3Client s3Client;

	@Bean
	public S3Client s3Client() {
		// SECURITY FIX: Validate credentials before creating client (fail-fast)
		if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
			throw new IllegalStateException(
					"AWS_ACCESS_KEY_ID is required but not configured. " +
					"Set it as an environment variable or in .brewer-s3.properties");
		}
		if (secretAccessKey == null || secretAccessKey.trim().isEmpty()) {
			throw new IllegalStateException(
					"AWS_SECRET_ACCESS_KEY is required but not configured. " +
					"Set it as an environment variable or in .brewer-s3.properties");
		}

		// AWS SDK v2: Create credentials
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

		// AWS SDK v2: Build S3 client with fluent API and explicit HTTP client configuration
		// FIX: Use httpClientBuilder to configure HTTP client inline (avoids type conversion issues)
		this.s3Client = S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.httpClientBuilder(UrlConnectionHttpClient.builder()
						.connectionTimeout(Duration.ofSeconds(10))
						.socketTimeout(Duration.ofSeconds(60)))
				.build();

		return this.s3Client;
	}

	/**
	 * Cleanup method to properly close S3Client and release resources.
	 *
	 * RESOURCE LEAK FIX: Phase 14 Post-Review
	 * Without proper cleanup, connection pools and threads may leak on application
	 * shutdown or bean recreation (e.g., during hot redeployments).
	 *
	 * This method is called by Spring when the bean is destroyed.
	 */
	@PreDestroy
	public void cleanup() {
		if (s3Client != null) {
			try {
				logger.info("Closing S3Client and releasing connection pool resources");
				s3Client.close();
			} catch (Exception e) {
				logger.error("Error closing S3Client", e);
			}
		}
	}

}