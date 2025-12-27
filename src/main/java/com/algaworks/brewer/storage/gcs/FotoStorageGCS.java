package com.algaworks.brewer.storage.gcs;

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
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

import net.coobird.thumbnailator.Thumbnails;

/**
 * Implementação de FotoStorage usando GCP Cloud Storage.
 *
 * Mirrors FotoStorageS3.java structure for consistency across multi-cloud deployments.
 *
 * Key differences from S3:
 * - Storage API instead of S3Client
 * - BlobInfo/BlobId instead of PutObjectRequest
 * - Storage.get() instead of GetObjectRequest
 * - BatchRequest for batch deletes
 * - Public URLs via Blob.getMediaLink() or signUrl()
 *
 * Security:
 * - Files are private by default (uniform bucket-level access)
 * - Access controlled via:
 *   1. Signed URLs (recommended)
 *   2. IAM permissions on bucket
 *   3. CDN with Cloud CDN
 */
@Profile("prod-gcp")
@Component
public class FotoStorageGCS implements FotoStorage {

	private static final Logger logger = LoggerFactory.getLogger(FotoStorageGCS.class);

	@Value("${gcp.storage.bucket}")
	private String bucket;

	@Autowired
	private Storage storage;

	@Override
	public String salvar(MultipartFile[] files) {
		String novoNome = null;
		if (files != null && files.length > 0) {
			MultipartFile arquivo = files[0];
			novoNome = renomearArquivo(arquivo.getOriginalFilename());

			try {
				// SECURITY: Files are private by default with uniform bucket-level access
				// Access should be controlled via:
				// 1. Signed URLs (recommended)
				// 2. IAM permissions
				// 3. Cloud CDN with signed cookies

				// Read file content once to avoid InputStream reuse issue
				byte[] fileBytes = arquivo.getBytes();

				enviarFoto(novoNome, fileBytes, arquivo.getContentType());
				enviarThumbnail(novoNome, fileBytes, arquivo.getContentType());
			} catch (IOException e) {
				throw new RuntimeException("Erro salvando arquivo no GCS", e);
			}
		}

		return novoNome;
	}

	@Override
	public byte[] recuperar(String foto) {
		try {
			BlobId blobId = BlobId.of(bucket, foto);
			Blob blob = storage.get(blobId);

			if (blob == null || !blob.exists()) {
				throw new RuntimeException(
						String.format("Foto '%s' não encontrada no bucket '%s'", foto, bucket));
			}

			// Get blob content as byte array
			return blob.getContent();

		} catch (StorageException e) {
			// ERROR HANDLING: Catch StorageException specifically to expose GCP error codes
			logger.error("GCS error recuperando foto '{}': {} (Code: {}, Reason: {})",
					foto, e.getMessage(), e.getCode(), e.getReason(), e);
			throw new RuntimeException(
					String.format("Erro recuperando foto do GCS: %s (Código: %d)",
							e.getMessage(), e.getCode()), e);
		} catch (Exception e) {
			logger.error("Erro inesperado recuperando foto '{}' do GCS", foto, e);
			throw new RuntimeException("Erro inesperado recuperando foto do GCS", e);
		}
	}

	@Override
	public byte[] recuperarThumbnail(String foto) {
		return recuperar(FotoStorage.THUMBNAIL_PREFIX + foto);
	}

	@Override
	public void excluir(String foto) {
		try {
			// Delete main photo
			BlobId mainBlobId = BlobId.of(bucket, foto);
			boolean mainDeleted = storage.delete(mainBlobId);

			// Delete thumbnail
			BlobId thumbBlobId = BlobId.of(bucket, THUMBNAIL_PREFIX + foto);
			boolean thumbDeleted = storage.delete(thumbBlobId);

			logger.debug("Deleted foto '{}': main={}, thumbnail={}", foto, mainDeleted, thumbDeleted);

		} catch (StorageException e) {
			logger.error("GCS error excluindo foto '{}': {} (Code: {})",
					foto, e.getMessage(), e.getCode(), e);
			throw new RuntimeException(
					String.format("Erro excluindo foto do GCS: %s", e.getMessage()), e);
		}
	}

	@Override
	public String getUrl(String foto) {
		if (!StringUtils.hasText(foto)) {
			return null;
		}

		// Generate public URL for the blob
		// Note: This returns the media link, which requires proper IAM permissions
		// For truly public access, use signed URLs or make bucket public
		BlobId blobId = BlobId.of(bucket, foto);
		Blob blob = storage.get(blobId);

		if (blob == null || !blob.exists()) {
			logger.warn("Blob '{}' não encontrado ao gerar URL", foto);
			return null;
		}

		// Return media link (requires authentication)
		// Alternative: Use blob.signUrl() for temporary signed URLs
		return blob.getMediaLink();
	}

	private void enviarFoto(String novoNome, byte[] fileBytes, String contentType) throws IOException {
		BlobId blobId = BlobId.of(bucket, novoNome);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
				.setContentType(contentType)
				.build();

		// Upload file to GCS
		try (InputStream is = new ByteArrayInputStream(fileBytes)) {
			storage.createFrom(blobInfo, is);
		}
	}

	private void enviarThumbnail(String novoNome, byte[] fileBytes, String contentType) throws IOException {
		// Generate thumbnail
		try (ByteArrayOutputStream os = new ByteArrayOutputStream();
			 InputStream inputStream = new ByteArrayInputStream(fileBytes)) {

			Thumbnails.of(inputStream).size(40, 68).toOutputStream(os);
			byte[] array = os.toByteArray();

			// Upload thumbnail to GCS
			BlobId blobId = BlobId.of(bucket, THUMBNAIL_PREFIX + novoNome);
			BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
					.setContentType(contentType)
					.build();

			try (InputStream is = new ByteArrayInputStream(array)) {
				storage.createFrom(blobInfo, is);
			}
		}
	}

}