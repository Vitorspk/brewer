package com.algaworks.brewer.storage.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.algaworks.brewer.storage.FotoStorage;

import net.coobird.thumbnailator.Thumbnails;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Implementação de FotoStorage usando AWS S3.
 *
 * MIGRATION: Phase 14 - Migrated from AWS SDK v1 to v2
 *
 * Key Changes:
 * - AmazonS3 → S3Client
 * - PutObjectRequest now uses builder pattern
 * - ObjectMetadata → PutObjectRequest.Builder contentType/contentLength
 * - S3Object → ResponseInputStream<GetObjectResponse>
 * - DeleteObjectsRequest uses builder with Delete object
 * - getUrl() uses GetUrlRequest builder
 * - IOUtils replaced with InputStream.readAllBytes()
 *
 * Benefits:
 * - Cleaner, more modern API with builders
 * - Better resource management
 * - Improved performance
 */
@Profile("prod")
@Component
public class FotoStorageS3 implements FotoStorage {

	private static final Logger logger = LoggerFactory.getLogger(FotoStorageS3.class);

	@Value("${aws.s3.bucket}")
	private String bucket;

	@Autowired
	private S3Client s3Client;

	@Override
	public String salvar(MultipartFile[] files) {
		String novoNome = null;
		if (files != null && files.length > 0) {
			MultipartFile arquivo = files[0];
			novoNome = renomearArquivo(arquivo.getOriginalFilename());

			try {
				// SECURITY FIX: Removed public ACL
				// Files are now private by default. Access should be controlled via:
				// 1. Presigned URLs (recommended)
				// 2. CloudFront with Origin Access Identity
				// 3. Bucket policy with specific conditions

				// Read file content once to avoid InputStream reuse issue
				byte[] fileBytes = arquivo.getBytes();

				enviarFoto(novoNome, fileBytes, arquivo.getContentType());
				enviarThumbnail(novoNome, fileBytes, arquivo.getContentType());
			} catch (IOException e) {
				throw new RuntimeException("Erro salvando arquivo no S3", e);
			}
		}

		return novoNome;
	}

	@Override
	public byte[] recuperar(String foto) {
		// AWS SDK v2: Use GetObjectRequest builder
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(foto)
				.build();

		try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest)) {
			// AWS SDK v2: Use standard InputStream.readAllBytes() instead of IOUtils
			return s3Object.readAllBytes();
		} catch (Exception e) {
			logger.error("Não foi possível recuperar a foto '{}' do S3.", foto, e);
			throw new RuntimeException("Erro recuperando foto do S3", e);
		}
	}

	@Override
	public byte[] recuperarThumbnail(String foto) {
		return recuperar(FotoStorage.THUMBNAIL_PREFIX + foto);
	}

	@Override
	public void excluir(String foto) {
		// AWS SDK v2: Use builder pattern for DeleteObjectsRequest
		DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
				.bucket(bucket)
				.delete(Delete.builder()
						.objects(
								ObjectIdentifier.builder().key(foto).build(),
								ObjectIdentifier.builder().key(THUMBNAIL_PREFIX + foto).build()
						)
						.build())
				.build();

		s3Client.deleteObjects(deleteRequest);
	}

	@Override
	public String getUrl(String foto) {
		if (!StringUtils.hasText(foto)) {
			return null;
		}

		// AWS SDK v2: Use GetUrlRequest builder
		GetUrlRequest getUrlRequest = GetUrlRequest.builder()
				.bucket(bucket)
				.key(foto)
				.build();

		return s3Client.utilities().getUrl(getUrlRequest).toString();
	}

	private void enviarFoto(String novoNome, byte[] fileBytes, String contentType) throws IOException {
		// AWS SDK v2: Use PutObjectRequest builder
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(novoNome)
				.contentType(contentType)
				.contentLength((long) fileBytes.length)
				.build();

		// ROBUSTNESS FIX: Use try-with-resources to ensure InputStream is closed
		try (InputStream is = new ByteArrayInputStream(fileBytes)) {
			// AWS SDK v2: Use RequestBody for upload
			s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(is, fileBytes.length));
		}
	}

	private void enviarThumbnail(String novoNome, byte[] fileBytes, String contentType) throws IOException {
		// ROBUSTNESS FIX: Use try-with-resources to ensure all streams are closed
		try (ByteArrayOutputStream os = new ByteArrayOutputStream();
			 InputStream inputStream = new ByteArrayInputStream(fileBytes)) {

			Thumbnails.of(inputStream).size(40, 68).toOutputStream(os);
			byte[] array = os.toByteArray();

			try (InputStream is = new ByteArrayInputStream(array)) {
				// AWS SDK v2: Use PutObjectRequest builder
				PutObjectRequest putObjectRequest = PutObjectRequest.builder()
						.bucket(bucket)
						.key(THUMBNAIL_PREFIX + novoNome)
						.contentType(contentType)
						.contentLength((long) array.length)
						.build();

				// AWS SDK v2: Use RequestBody for upload
				s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(is, array.length));
			}
		}
	}

}