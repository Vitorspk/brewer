package com.algaworks.brewer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

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
	public AmazonS3 amazonS3() {
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

		BasicAWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
		return AmazonS3ClientBuilder.standard()
				.withRegion(region)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();
	}

}
