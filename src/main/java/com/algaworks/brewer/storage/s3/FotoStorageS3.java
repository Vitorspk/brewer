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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;

import net.coobird.thumbnailator.Thumbnails;

@Profile("prod")
@Component
public class FotoStorageS3 implements FotoStorage {

	private static final Logger logger = LoggerFactory.getLogger(FotoStorageS3.class);

	@Value("${aws.s3.bucket}")
	private String bucket;

	@Autowired
	private AmazonS3 amazonS3;
	
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
		try (com.amazonaws.services.s3.model.S3Object s3Object = amazonS3.getObject(bucket, foto);
				InputStream is = s3Object.getObjectContent()) {
			return IOUtils.toByteArray(is);
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
		amazonS3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(foto, THUMBNAIL_PREFIX + foto));
	}

	@Override
	public String getUrl(String foto) {
		if (!StringUtils.isEmpty(foto)) {
			return amazonS3.getUrl(bucket, foto).toString();
		}
		return null;
	}
	
	private ObjectMetadata enviarFoto(String novoNome, byte[] fileBytes, String contentType)
			throws IOException {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType(contentType);
		metadata.setContentLength(fileBytes.length);
		// ROBUSTNESS FIX: Use try-with-resources to ensure InputStream is closed
		try (InputStream is = new ByteArrayInputStream(fileBytes)) {
			// Private by default - no ACL specified
			amazonS3.putObject(new PutObjectRequest(bucket, novoNome, is, metadata));
		}
		return metadata;
	}

	private void enviarThumbnail(String novoNome, byte[] fileBytes, String contentType) throws IOException {
		// ROBUSTNESS FIX: Use try-with-resources to ensure all streams are closed
		try (ByteArrayOutputStream os = new ByteArrayOutputStream();
			 InputStream inputStream = new ByteArrayInputStream(fileBytes)) {

			Thumbnails.of(inputStream).size(40, 68).toOutputStream(os);
			byte[] array = os.toByteArray();

			try (InputStream is = new ByteArrayInputStream(array)) {
				ObjectMetadata thumbMetadata = new ObjectMetadata();
				thumbMetadata.setContentType(contentType);
				thumbMetadata.setContentLength(array.length);
				// Private by default - no ACL specified
				amazonS3.putObject(new PutObjectRequest(bucket, THUMBNAIL_PREFIX + novoNome, is, thumbMetadata));
			}
		}
	}

}
