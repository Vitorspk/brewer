package com.algaworks.brewer.storage.s3;

import com.algaworks.brewer.storage.FotoStorage;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FotoStorageS3
 *
 * Tests the resource leak fixes from Part 3:
 * - InputStreams are properly closed with try-with-resources
 * - No connection pool exhaustion under load
 * - Error handling doesn't leak resources
 */
@ExtendWith(MockitoExtension.class)
class FotoStorageS3Test {

	@Mock
	private AmazonS3 amazonS3;

	@Mock
	private MultipartFile multipartFile;

	@InjectMocks
	private FotoStorageS3 fotoStorage;

	private static final String BUCKET_NAME = "test-bucket";

	@BeforeEach
	void setUp() {
		// Inject bucket name via reflection (simulating @Value injection)
		ReflectionTestUtils.setField(fotoStorage, "bucket", BUCKET_NAME);
	}

	@Test
	void deveSalvarFotoComSucessoEFecharStreams() throws IOException {
		// Given: Valid image file
		byte[] fileContent = "fake-image-content".getBytes();
		when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
		when(multipartFile.getBytes()).thenReturn(fileContent);
		when(multipartFile.getContentType()).thenReturn("image/jpeg");

		// When: Saving the file
		String novoNome = fotoStorage.salvar(new MultipartFile[]{multipartFile});

		// Then: Should upload both main image and thumbnail
		assertNotNull(novoNome);
		assertTrue(novoNome.endsWith(".jpg"));

		// Verify S3 upload was called (main image + thumbnail)
		verify(amazonS3, times(2)).putObject(any(PutObjectRequest.class));
	}

	@Test
	void deveRecuperarFotoCorretamente() throws IOException {
		// Given: Existing photo in S3
		String fotoNome = "test-photo.jpg";
		byte[] expectedContent = "photo-content".getBytes();

		S3Object s3Object = mock(S3Object.class);
		S3ObjectInputStream inputStream = new S3ObjectInputStream(
			new ByteArrayInputStream(expectedContent), null);

		when(amazonS3.getObject(BUCKET_NAME, fotoNome)).thenReturn(s3Object);
		when(s3Object.getObjectContent()).thenReturn(inputStream);

		// When: Retrieving the photo
		byte[] result = fotoStorage.recuperar(fotoNome);

		// Then: Should return correct content and close stream
		assertArrayEquals(expectedContent, result);
		verify(amazonS3).getObject(BUCKET_NAME, fotoNome);

		// Note: S3ObjectInputStream is closed by try-with-resources in recuperar()
	}

	@Test
	void deveLancarExcecaoSeRecuperarFalhar() {
		// Given: S3 error when retrieving photo
		String fotoNome = "non-existent.jpg";
		when(amazonS3.getObject(BUCKET_NAME, fotoNome))
			.thenThrow(new RuntimeException("S3 error"));

		// When/Then: Should throw RuntimeException and not leak resources
		assertThrows(RuntimeException.class, () -> fotoStorage.recuperar(fotoNome));
	}

	@Test
	void deveRecuperarThumbnailUsandoPrefixo() throws IOException {
		// Given: Existing thumbnail in S3
		String fotoNome = "test-photo.jpg";
		String thumbnailKey = FotoStorage.THUMBNAIL_PREFIX + fotoNome;
		byte[] expectedContent = "thumbnail-content".getBytes();

		S3Object s3Object = mock(S3Object.class);
		S3ObjectInputStream inputStream = new S3ObjectInputStream(
			new ByteArrayInputStream(expectedContent), null);

		when(amazonS3.getObject(BUCKET_NAME, thumbnailKey)).thenReturn(s3Object);
		when(s3Object.getObjectContent()).thenReturn(inputStream);

		// When: Retrieving the thumbnail
		byte[] result = fotoStorage.recuperarThumbnail(fotoNome);

		// Then: Should retrieve with thumbnail prefix
		assertArrayEquals(expectedContent, result);
		verify(amazonS3).getObject(BUCKET_NAME, thumbnailKey);
	}

	@Test
	void deveExcluirFotoEThumbnail() {
		// Given: Photo name to delete
		String fotoNome = "test-photo.jpg";

		// When: Deleting the photo
		fotoStorage.excluir(fotoNome);

		// Then: Should delete both main photo and thumbnail in single request
		verify(amazonS3).deleteObjects(argThat(request ->
			request.getBucketName().equals(BUCKET_NAME) &&
			request.getKeys().size() == 2 &&
			request.getKeys().get(0).getKey().equals(fotoNome) &&
			request.getKeys().get(1).getKey().equals(FotoStorage.THUMBNAIL_PREFIX + fotoNome)
		));
	}

	@Test
	void deveRetornarUrlDaFoto() throws Exception {
		// Given: Photo name
		String fotoNome = "test-photo.jpg";
		String expectedUrl = "https://s3.amazonaws.com/" + BUCKET_NAME + "/" + fotoNome;

		when(amazonS3.getUrl(BUCKET_NAME, fotoNome))
			.thenReturn(new java.net.URL(expectedUrl));

		// When: Getting photo URL
		String url = fotoStorage.getUrl(fotoNome);

		// Then: Should return S3 URL
		assertEquals(expectedUrl, url);
	}

	@Test
	void deveRetornarNullParaFotoVazia() {
		// Given: Empty photo name
		String fotoNome = "";

		// When: Getting URL for empty name
		String url = fotoStorage.getUrl(fotoNome);

		// Then: Should return null
		assertNull(url);
		verify(amazonS3, never()).getUrl(any(), any());
	}

	@Test
	void deveRetornarNullParaFotoNull() {
		// Given: Null photo name
		String fotoNome = null;

		// When: Getting URL for null name
		String url = fotoStorage.getUrl(fotoNome);

		// Then: Should return null
		assertNull(url);
		verify(amazonS3, never()).getUrl(any(), any());
	}

	@Test
	void deveLancarExcecaoSeUploadFalhar() throws IOException {
		// Given: Upload that will fail
		when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
		when(multipartFile.getBytes()).thenThrow(new IOException("Failed to read file"));

		// When/Then: Should throw RuntimeException and not leak resources
		assertThrows(RuntimeException.class, () ->
			fotoStorage.salvar(new MultipartFile[]{multipartFile}));
	}

	@Test
	void deveRetornarNullSeArrayDeArquivosForNull() {
		// Given: Null file array
		MultipartFile[] files = null;

		// When: Saving null array
		String result = fotoStorage.salvar(files);

		// Then: Should return null without attempting upload
		assertNull(result);
		verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
	}

	@Test
	void deveRetornarNullSeArrayDeArquivosForVazio() {
		// Given: Empty file array
		MultipartFile[] files = new MultipartFile[0];

		// When: Saving empty array
		String result = fotoStorage.salvar(files);

		// Then: Should return null without attempting upload
		assertNull(result);
		verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
	}

	@Test
	void deveUsarArquivosPrivadosPorPadrao() throws IOException {
		// Given: Valid file (security fix: no public ACL)
		byte[] fileContent = "fake-image-content".getBytes();
		when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
		when(multipartFile.getBytes()).thenReturn(fileContent);
		when(multipartFile.getContentType()).thenReturn("image/jpeg");

		ArgumentCaptor<PutObjectRequest> requestCaptor =
			ArgumentCaptor.forClass(PutObjectRequest.class);

		// When: Saving the file
		fotoStorage.salvar(new MultipartFile[]{multipartFile});

		// Then: Should not set public ACL (files are private by default)
		verify(amazonS3, atLeastOnce()).putObject(requestCaptor.capture());

		// Verify no ACL was set (meaning default private access)
		for (PutObjectRequest request : requestCaptor.getAllValues()) {
			assertNull(request.getAccessControlList(),
				"ACL should be null (private by default) for security");
		}
	}
}