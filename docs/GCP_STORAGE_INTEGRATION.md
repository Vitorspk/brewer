# GCP Cloud Storage Integration - Brewer Application

Documentação completa sobre a integração do Google Cloud Storage no projeto Brewer para armazenamento de fotos.

## Visão Geral

O projeto Brewer agora suporta **multi-cloud storage**, podendo ser deployed tanto na AWS (S3) quanto no GCP (Cloud Storage), com implementações separadas mas compatíveis.

### Arquitetura

```
FotoStorage (Interface)
├── FotoStorageLocal  (@Profile "!prod")       - Desenvolvimento local
├── FotoStorageS3     (@Profile "prod")        - Produção AWS
└── FotoStorageGCS    (@Profile "prod-gcp")    - Produção GCP  👈 NOVO
```

## Arquivos Criados

### 1. Dependências Maven

**Arquivo**: [`pom.xml`](../pom.xml)

```xml
<!-- GCP Cloud Storage - Multi-cloud support -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-storage</artifactId>
    <version>2.43.2</version>
</dependency>
```

### 2. Configuração do Cliente GCS

**Arquivo**: [`src/main/java/com/algaworks/brewer/config/GCSConfig.java`](../src/main/java/com/algaworks/brewer/config/GCSConfig.java)

Configura o cliente do Google Cloud Storage com suporte a múltiplos métodos de autenticação:

**Métodos de Autenticação (ordem de precedência):**

1. **Inline JSON** (`GCP_CREDENTIALS_JSON`) - Recomendado para containers
   ```bash
   export GCP_CREDENTIALS_JSON=$(cat service-account-key.json)
   ```

2. **File path** (`GOOGLE_APPLICATION_CREDENTIALS`) - Recomendado para desenvolvimento local
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
   ```

3. **Application Default Credentials (ADC)** - Recomendado para GKE com Workload Identity
   - Não requer variável de ambiente
   - Credenciais automaticamente fornecidas pelo GKE

### 3. Implementação do Storage

**Arquivo**: [`src/main/java/com/algaworks/brewer/storage/gcs/FotoStorageGCS.java`](../src/main/java/com/algaworks/brewer/storage/gcs/FotoStorageGCS.java)

Implementação do `FotoStorage` para GCP Cloud Storage, espelhando a estrutura do `FotoStorageS3` para consistência.

**Características principais:**

- ✅ Upload de fotos e thumbnails
- ✅ Recuperação de fotos
- ✅ Deleção de fotos (main + thumbnail)
- ✅ Geração de URLs públicas
- ✅ Tratamento de erros específico do GCS
- ✅ Segurança: arquivos privados por padrão

### 4. Configuração de Produção GCP

**Arquivo**: [`src/main/resources/application-prod-gcp.properties`](../src/main/resources/application-prod-gcp.properties)

Profile Spring Boot específico para deployment no GCP.

**Variáveis de ambiente obrigatórias:**

```bash
# Database
DATABASE_URL=jdbc:mysql://CLOUD_SQL_IP:3306/brewer?useSSL=true
DATABASE_USERNAME=brewer_user
DATABASE_PASSWORD=secure_password

# GCP Cloud Storage
GCP_PROJECT_ID=vschiavo-home
GCP_STORAGE_BUCKET=brewer-fotos

# Credentials (escolher um método)
GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json
# OU
GCP_CREDENTIALS_JSON=$(cat service-account-key.json)

# Spring Profile
SPRING_PROFILES_ACTIVE=prod-gcp
```

### 5. Kubernetes Secrets

**Arquivo**: [`k8s/base/secret-gcp.yaml.template`](../k8s/base/secret-gcp.yaml.template)

Template de secret para deployment no GKE com três métodos suportados:

1. **Método 1: Inline JSON** (recomendado para CI/CD)
2. **Método 2: File-based credentials**
3. **Método 3: Workload Identity** (RECOMENDADO para produção)

## Configuração e Deployment

### Passo 1: Criar Bucket no GCP

O bucket já foi criado seguindo [`docs/GCP_STORAGE_SETUP.md`](GCP_STORAGE_SETUP.md):

```bash
PROJECT_ID="vschiavo-home"
BUCKET_NAME="brewer-fotos"
REGION="southamerica-east1"

gcloud storage buckets create gs://${BUCKET_NAME} \
  --project=${PROJECT_ID} \
  --location=${REGION} \
  --uniform-bucket-level-access
```

**Status**: ✅ Bucket criado e configurado

### Passo 2: Configurar Permissões

```bash
SA_EMAIL="github-actions-terraform@vschiavo-home.iam.gserviceaccount.com"

gcloud storage buckets add-iam-policy-binding gs://${BUCKET_NAME} \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/storage.objectAdmin"
```

**Status**: ✅ Permissões configuradas

### Passo 3: Configurar Secrets no Kubernetes

#### Opção A: Usando kubectl (com inline JSON)

```bash
kubectl create secret generic brewer-secrets \
  --from-literal=DATABASE_URL='jdbc:mysql://CLOUD_SQL_IP:3306/brewer?useSSL=true&requireSSL=true&serverTimezone=UTC' \
  --from-literal=DATABASE_USERNAME='brewer_user' \
  --from-literal=DATABASE_PASSWORD='secure_password' \
  --from-literal=GCP_PROJECT_ID='vschiavo-home' \
  --from-literal=GCP_STORAGE_BUCKET='brewer-fotos' \
  --from-literal=GCP_CREDENTIALS_JSON="$(cat service-account-key.json)" \
  --from-literal=SPRING_PROFILES_ACTIVE='prod-gcp' \
  --namespace=brewer
```

#### Opção B: Usando Workload Identity (RECOMENDADO)

```bash
# 1. Habilitar Workload Identity no cluster
gcloud container clusters update gke-dev \
  --workload-pool=vschiavo-home.svc.id.goog \
  --region=southamerica-east1

# 2. Criar Kubernetes Service Account
kubectl create serviceaccount brewer-ksa --namespace=brewer

# 3. Vincular KSA com GSA
gcloud iam service-accounts add-iam-policy-binding \
  github-actions-terraform@vschiavo-home.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:vschiavo-home.svc.id.goog[brewer/brewer-ksa]"

# 4. Anotar o KSA
kubectl annotate serviceaccount brewer-ksa \
  --namespace=brewer \
  iam.gke.io/gcp-service-account=github-actions-terraform@vschiavo-home.iam.gserviceaccount.com

# 5. Criar secret (SEM credenciais GCP)
kubectl create secret generic brewer-secrets \
  --from-literal=DATABASE_URL='...' \
  --from-literal=DATABASE_USERNAME='...' \
  --from-literal=DATABASE_PASSWORD='...' \
  --from-literal=GCP_PROJECT_ID='vschiavo-home' \
  --from-literal=GCP_STORAGE_BUCKET='brewer-fotos' \
  --from-literal=SPRING_PROFILES_ACTIVE='prod-gcp' \
  --namespace=brewer

# 6. Atualizar deployment.yaml
# spec:
#   serviceAccountName: brewer-ksa
```

### Passo 4: Build e Deploy

O build já funciona automaticamente com o workflow existente em [`.github/workflows/deploy-to-gke.yml`](../.github/workflows/deploy-to-gke.yml).

**O que acontece no build:**

1. Maven baixa a dependência `google-cloud-storage`
2. Compila `GCSConfig.java` e `FotoStorageGCS.java`
3. Inclui `application-prod-gcp.properties` no JAR
4. Docker image é criado com todas as dependências GCP
5. Deploy no GKE com profile `prod-gcp`

## Comparação: AWS S3 vs GCP Cloud Storage

| Aspecto | AWS S3 | GCP Cloud Storage |
|---------|--------|-------------------|
| **Profile** | `prod` | `prod-gcp` |
| **Classe Config** | `S3Config.java` | `GCSConfig.java` |
| **Classe Storage** | `FotoStorageS3.java` | `FotoStorageGCS.java` |
| **Cliente SDK** | `S3Client` | `Storage` |
| **Bucket Config** | `aws.s3.bucket` | `gcp.storage.bucket` |
| **Credenciais** | Access Key + Secret | Service Account JSON |
| **Autenticação K8s** | IAM Role (IRSA) | Workload Identity |
| **Região** | `sa-east-1` | `southamerica-east1` |
| **URL pública** | `s3Client.utilities().getUrl()` | `blob.getMediaLink()` |
| **Dependência** | `software.amazon.awssdk:s3` | `com.google.cloud:google-cloud-storage` |

## Operações Suportadas

### 1. Upload de Foto

```java
@Autowired
private FotoStorage fotoStorage; // Injeta FotoStorageGCS quando prod-gcp está ativo

String nomeArquivo = fotoStorage.salvar(files);
// Salva foto principal e thumbnail automaticamente
```

**Implementação GCS:**
- Cria `BlobInfo` com `contentType`
- Usa `storage.createFrom(blobInfo, inputStream)`
- Gera thumbnail com Thumbnailator
- Salva thumbnail com prefixo `thumbnail.`

### 2. Recuperação de Foto

```java
byte[] fotoBytes = fotoStorage.recuperar("nome-foto.jpg");
byte[] thumbnailBytes = fotoStorage.recuperarThumbnail("nome-foto.jpg");
```

**Implementação GCS:**
- Usa `BlobId.of(bucket, key)`
- Recupera com `storage.get(blobId).getContent()`
- Tratamento de erro específico para `StorageException`

### 3. Deleção de Foto

```java
fotoStorage.excluir("nome-foto.jpg");
// Deleta foto principal E thumbnail
```

**Implementação GCS:**
- Deleta dois blobs: foto principal + thumbnail
- Usa `storage.delete(blobId)` para cada um

### 4. Obter URL Pública

```java
String url = fotoStorage.getUrl("nome-foto.jpg");
// Retorna: https://storage.googleapis.com/bucket/foto.jpg
```

**Implementação GCS:**
- Usa `blob.getMediaLink()` para URL pública
- Requer permissões IAM adequadas
- Alternativa: `blob.signUrl()` para URLs assinadas temporárias

## Segurança

### Arquivos Privados por Padrão

Ambas implementações (S3 e GCS) mantêm arquivos privados por padrão:

- ✅ **S3**: Sem ACL público, removido em Phase 14
- ✅ **GCS**: Uniform bucket-level access habilitado

### Controle de Acesso

**Métodos recomendados:**

1. **Signed URLs** (temporárias)
   ```java
   // GCS
   URL signedUrl = blob.signUrl(1, TimeUnit.HOURS);

   // S3
   URL presignedUrl = s3Presigner.presignGetObject(request);
   ```

2. **IAM Permissions** (permanentes)
   - S3: Bucket Policy ou IAM Role
   - GCS: IAM binding no bucket

3. **CDN** (caching + segurança)
   - S3: CloudFront com OAI
   - GCS: Cloud CDN com signed cookies

### Service Account Permissions

A Service Account `github-actions-terraform@vschiavo-home.iam.gserviceaccount.com` possui:

- ✅ `roles/storage.objectAdmin` no bucket `brewer-fotos`
- ✅ Permissões para criar, ler, atualizar e deletar objetos
- ✅ Já configurada no GitHub Actions como `GCP_SA_KEY`

## Testes

### Teste Local (sem GCP)

```bash
# Usa FotoStorageLocal (development)
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

### Teste com GCP (local)

```bash
# Configura credenciais
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
export GCP_PROJECT_ID=vschiavo-home
export GCP_STORAGE_BUCKET=brewer-fotos
export SPRING_PROFILES_ACTIVE=prod-gcp

# Configura database
export DATABASE_URL=jdbc:mysql://localhost:3306/brewer
export DATABASE_USERNAME=root
export DATABASE_PASSWORD=password

# Executa
mvn spring-boot:run
```

### Teste no GKE

O deployment via GitHub Actions automaticamente testa a integração:

```yaml
# .github/workflows/deploy-to-gke.yml
- name: Run tests
  env:
    TEST_DB_URL: ${{ secrets.TEST_DB_URL_GCP }}
    TEST_DB_USER: ${{ secrets.TEST_DB_USER_GCP }}
    TEST_DB_PASSWORD: ${{ secrets.TEST_DB_PASSWORD_GCP }}
  run: mvn test -B
```

## Troubleshooting

### Erro: "GCP_PROJECT_ID is required"

```
IllegalStateException: GCP_PROJECT_ID is required but not configured
```

**Solução**: Configurar variável de ambiente

```bash
export GCP_PROJECT_ID=vschiavo-home
```

### Erro: "Failed to initialize GCP Cloud Storage client"

```
IOException: Failed to initialize GCP Cloud Storage client
```

**Possíveis causas:**

1. Service account JSON inválido
2. Arquivo de credenciais não encontrado
3. Permissões insuficientes

**Solução**: Verificar credenciais

```bash
# Testar autenticação
gcloud auth activate-service-account --key-file=service-account-key.json

# Verificar permissões
gcloud storage buckets get-iam-policy gs://brewer-fotos
```

### Erro: "Foto não encontrada no bucket"

```
RuntimeException: Foto 'xyz.jpg' não encontrada no bucket 'brewer-fotos'
```

**Solução**: Verificar se arquivo existe

```bash
gcloud storage ls gs://brewer-fotos/xyz.jpg
```

### Erro: "Access Denied" (403)

```
StorageException: 403 Forbidden
```

**Solução**: Verificar permissões IAM

```bash
# Verificar IAM policy do bucket
gcloud storage buckets get-iam-policy gs://brewer-fotos

# Adicionar permissão se necessário
gcloud storage buckets add-iam-policy-binding gs://brewer-fotos \
  --member="serviceAccount:SA_EMAIL" \
  --role="roles/storage.objectAdmin"
```

## Monitoramento

### Logs de Aplicação

```bash
# Ver logs do pod no GKE
kubectl logs -f deployment/brewer-app -n brewer

# Filtrar logs do GCS
kubectl logs deployment/brewer-app -n brewer | grep GCSConfig
kubectl logs deployment/brewer-app -n brewer | grep FotoStorageGCS
```

### Métricas do Cloud Storage

```bash
# Ver métricas de uso do bucket
gcloud monitoring metrics-descriptors list \
  --filter="metric.type:storage.googleapis.com"

# Ver tamanho do bucket
gcloud storage du -s gs://brewer-fotos
```

### Auditoria

```bash
# Ver logs de acesso ao bucket
gcloud logging read "resource.type=gcs_bucket AND resource.labels.bucket_name=brewer-fotos" \
  --limit 50 \
  --format json
```

## Migração de AWS para GCP

Se você tem dados existentes na AWS S3 e quer migrar para GCP:

### Opção 1: Transfer Service (Recomendado)

```bash
# Usar GCP Transfer Service via console ou gcloud
# https://console.cloud.google.com/transfer/cloud

# Exemplo via CLI
gcloud transfer jobs create s3://brewer-fotos-aws gs://brewer-fotos \
  --source-creds-file=aws-credentials.json
```

### Opção 2: gsutil rsync

```bash
# Sincronizar de S3 para GCS
gsutil -m rsync -r s3://brewer-fotos-aws gs://brewer-fotos
```

### Opção 3: Script customizado

```bash
# Download de S3
aws s3 sync s3://brewer-fotos-aws /tmp/fotos

# Upload para GCS
gcloud storage cp -r /tmp/fotos gs://brewer-fotos

# Limpar
rm -rf /tmp/fotos
```

## Próximos Passos

- [ ] Implementar signed URLs para acesso temporário
- [ ] Configurar Cloud CDN para caching de fotos
- [ ] Implementar lifecycle policies para arquivamento
- [ ] Adicionar testes de integração específicos para GCS
- [ ] Configurar alertas de custo e uso
- [ ] Implementar backup automático cross-region

## Referências

- [GCP Cloud Storage Documentation](https://cloud.google.com/storage/docs)
- [GCP Cloud Storage Java Client](https://cloud.google.com/java/docs/reference/google-cloud-storage/latest/overview)
- [Workload Identity Best Practices](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity)
- [GCP Storage Pricing](https://cloud.google.com/storage/pricing)
- [Comparison: AWS S3 vs GCP Cloud Storage](https://cloud.google.com/storage/docs/aws-s3-migration)

---

**Documentação criada em**: 2025-12-27
**Versão**: 1.0
**Autor**: Claude (Anthropic)
**Projeto**: Brewer - Sistema de Gerenciamento de Cervejaria