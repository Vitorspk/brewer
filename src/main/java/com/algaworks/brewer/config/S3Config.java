package com.algaworks.brewer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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

	@Value("${AWS_ACCESS_KEY_ID}")
	private String accessKeyId;

	@Value("${AWS_SECRET_ACCESS_KEY}")
	private String secretAccessKey;

	@Value("${AWS_REGION:sa-east-1}")
	private String region;

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

		// AWS SDK v2: Build S3 client with fluent API
		return S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.build();
	}

}