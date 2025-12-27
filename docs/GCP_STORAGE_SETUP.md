# GCP Cloud Storage Setup - Brewer Photo Storage

Documentação para configurar o bucket do Google Cloud Storage para armazenamento de fotos em produção.

## Pré-requisitos

- Google Cloud SDK (gcloud) instalado e configurado
- Projeto GCP criado
- Credenciais GCP com permissões para Cloud Storage
- Região definida (padrão: southamerica-east1 - São Paulo)

## 1. Criar Bucket no Cloud Storage

```bash
# Definir variáveis
PROJECT_ID="seu-project-id"
BUCKET_NAME="brewer-fotos"
REGION="southamerica-east1"

# Criar bucket na região southamerica-east1 (São Paulo)
gcloud storage buckets create gs://${BUCKET_NAME} \
  --project=${PROJECT_ID} \
  --location=${REGION} \
  --uniform-bucket-level-access
```

**Nota**: `uniform-bucket-level-access` é equivalente ao "Block Public Access" da AWS e é recomendado para segurança.

## 2. Configurar Permissões do Bucket

```bash
# Garantir que o bucket não seja público por padrão
gcloud storage buckets update gs://${BUCKET_NAME} \
  --no-public-access-prevention

# Verificar configuração
gcloud storage buckets describe gs://${BUCKET_NAME}
```

## 3. Configurar CORS (se necessário para upload direto do browser)

```bash
# Criar arquivo cors.json
cat > /tmp/cors.json <<'EOF'
[
  {
    "origin": ["http://localhost:8080", "https://brewer.virosistemas.com"],
    "method": ["GET", "POST", "PUT", "DELETE"],
    "responseHeader": ["Content-Type"],
    "maxAgeSeconds": 3000
  }
]
EOF

# Aplicar configuração CORS
gcloud storage buckets update gs://${BUCKET_NAME} \
  --cors-file=/tmp/cors.json
```

## 4. Configurar Acesso Público de Leitura (Opcional)

```bash
# Permitir leitura pública de todos os objetos no bucket
gsutil iam ch allUsers:objectViewer gs://${BUCKET_NAME}
```

**⚠️ ATENÇÃO**: Isso torna todas as fotos publicamente acessíveis. Use apenas se necessário.

**Alternativa mais segura**: Usar URLs assinadas geradas pela aplicação.

## 5. Configurar Lifecycle (Opcional - Limpeza Automática)

```bash
# Criar regra para deletar uploads incompletos após 7 dias
cat > /tmp/lifecycle.json <<'EOF'
{
  "lifecycle": {
    "rule": [
      {
        "action": {
          "type": "Delete"
        },
        "condition": {
          "age": 7,
          "matchesPrefix": ["temp/"],
          "matchesSuffix": [".tmp"]
        }
      },
      {
        "action": {
          "type": "AbortIncompleteMultipartUpload"
        },
        "condition": {
          "age": 7
        }
      }
    ]
  }
}
EOF

# Aplicar lifecycle
gcloud storage buckets update gs://${BUCKET_NAME} \
  --lifecycle-file=/tmp/lifecycle.json
```

## 6. Criar Service Account para Aplicação

```bash
# Definir variáveis
SA_NAME="brewer-storage"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

# Criar Service Account
gcloud iam service-accounts create ${SA_NAME} \
  --display-name="Brewer Storage Service Account" \
  --description="Service account for Brewer application to access Cloud Storage" \
  --project=${PROJECT_ID}

# Atribuir permissão ao bucket (Storage Object Admin)
gcloud storage buckets add-iam-policy-binding gs://${BUCKET_NAME} \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/storage.objectAdmin"

# Criar chave JSON
gcloud iam service-accounts keys create brewer-storage-key.json \
  --iam-account=${SA_EMAIL} \
  --project=${PROJECT_ID}
```

**⚠️ IMPORTANTE**: Salve o arquivo `brewer-storage-key.json` de forma segura. Você precisará dele na aplicação.

### Permissões Alternativas (Mais Restritivas)

Se quiser permissões mais granulares:

```bash
# Criar role customizada com permissões mínimas
gcloud iam roles create brewerStorageRole \
  --project=${PROJECT_ID} \
  --title="Brewer Storage Role" \
  --description="Custom role for Brewer storage operations" \
  --permissions="storage.objects.create,storage.objects.delete,storage.objects.get,storage.objects.list" \
  --stage=GA

# Atribuir role customizada
gcloud storage buckets add-iam-policy-binding gs://${BUCKET_NAME} \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="projects/${PROJECT_ID}/roles/brewerStorageRole"
```

## 7. Configurar Variáveis de Ambiente na Aplicação

```bash
# Em produção, configure estas variáveis de ambiente:
export GCP_PROJECT_ID=seu-project-id
export GCP_STORAGE_BUCKET=brewer-fotos
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/brewer-storage-key.json

# OU use o conteúdo da chave JSON diretamente (recomendado para containers)
export GCP_CREDENTIALS_JSON=$(cat brewer-storage-key.json)

# Ativar profile de produção
export SPRING_PROFILES_ACTIVE=prod-gcp
```

## 8. Verificar Configuração

```bash
# Listar buckets
gcloud storage buckets list --project=${PROJECT_ID}

# Testar upload
echo "test" > /tmp/test.txt
gcloud storage cp /tmp/test.txt gs://${BUCKET_NAME}/test.txt

# Verificar arquivo
gcloud storage ls gs://${BUCKET_NAME}/

# Obter URL do arquivo
gcloud storage objects describe gs://${BUCKET_NAME}/test.txt --format="value(mediaLink)"

# Deletar arquivo de teste
gcloud storage rm gs://${BUCKET_NAME}/test.txt
```

## 9. Configuração no application-prod-gcp.properties

```properties
# Profile prod-gcp para GCP
spring.profiles.active=prod-gcp

# Configurações GCP Cloud Storage
gcp.project-id=${GCP_PROJECT_ID}
gcp.storage.bucket=${GCP_STORAGE_BUCKET:brewer-fotos}
gcp.credentials.location=${GOOGLE_APPLICATION_CREDENTIALS}

# Alternativamente, usar JSON inline (para containers/Kubernetes)
# gcp.credentials.json=${GCP_CREDENTIALS_JSON}
```

## 10. Configuração no Kubernetes (Secrets)

```bash
# Criar secret no cluster GKE com a chave da Service Account
kubectl create secret generic brewer-storage-credentials \
  --from-file=key.json=brewer-storage-key.json \
  --namespace=brewer

# OU criar a partir das variáveis de ambiente
kubectl create secret generic brewer-gcp-config \
  --from-literal=GCP_PROJECT_ID='seu-project-id' \
  --from-literal=GCP_STORAGE_BUCKET='brewer-fotos' \
  --from-file=GCP_CREDENTIALS_JSON=brewer-storage-key.json \
  --namespace=brewer
```

**No deployment.yaml:**
```yaml
env:
- name: GCP_PROJECT_ID
  valueFrom:
    secretKeyRef:
      name: brewer-gcp-config
      key: GCP_PROJECT_ID
- name: GCP_STORAGE_BUCKET
  valueFrom:
    secretKeyRef:
      name: brewer-gcp-config
      key: GCP_STORAGE_BUCKET
- name: GOOGLE_APPLICATION_CREDENTIALS
  value: /var/secrets/google/key.json
volumeMounts:
- name: gcp-credentials
  mountPath: /var/secrets/google
  readOnly: true
volumes:
- name: gcp-credentials
  secret:
    secretName: brewer-storage-credentials
```

## 11. Comandos Úteis

```bash
# Ver tamanho do bucket
gcloud storage du -s gs://${BUCKET_NAME}

# Listar objetos com detalhes
gcloud storage ls -l gs://${BUCKET_NAME}

# Sincronizar fotos locais para Cloud Storage
gcloud storage rsync ~/.brewerfotos gs://${BUCKET_NAME} --recursive

# Baixar backup de todas as fotos
gcloud storage rsync gs://${BUCKET_NAME} ~/backup-brewer-fotos --recursive

# Deletar bucket (cuidado!)
gcloud storage buckets delete gs://${BUCKET_NAME}

# Gerar URL assinada (válida por 1 hora)
gcloud storage sign-url gs://${BUCKET_NAME}/foto.jpg \
  --duration=1h \
  --private-key-file=brewer-storage-key.json
```

## 12. Monitoramento e Métricas

```bash
# Ver métricas de uso do bucket
gcloud monitoring metrics-descriptors list \
  --filter="metric.type:storage.googleapis.com"

# Configurar alertas de custo
gcloud alpha billing budgets create \
  --billing-account=BILLING_ACCOUNT_ID \
  --display-name="Brewer Storage Budget" \
  --budget-amount=10 \
  --threshold-rule=percent=80
```

## Custos Estimados (southamerica-east1)

- **Armazenamento Standard**: ~$0.020 USD por GB/mês
- **Armazenamento Nearline**: ~$0.010 USD por GB/mês (acesso < 1x/mês)
- **Operações Classe A** (inserir, listar): $0.05 por 10.000 operações
- **Operações Classe B** (ler): $0.004 por 10.000 operações
- **Egress (saída de dados)**: $0.12 USD por GB (após 1GB grátis/mês)

**Exemplo**: 10.000 fotos (~5GB) = ~$0.10 USD/mês + operações

### Comparação com AWS S3 (sa-east-1):
```
                 GCP             AWS
Armazenamento:  $0.020/GB/mês   $0.023/GB/mês  ← GCP mais barato
GET ops:        $0.004/10k      $0.0004/10k    ← AWS mais barato
PUT ops:        $0.05/10k       $0.005/10k     ← AWS mais barato
Egress:         $0.12/GB        $0.09/GB       ← AWS mais barato
```

## Troubleshooting

### Erro: "Permission Denied"
```bash
# Verificar permissões da Service Account
gcloud storage buckets get-iam-policy gs://${BUCKET_NAME}

# Verificar se as credenciais estão corretas
gcloud auth list
gcloud config get-value project

# Testar acesso ao bucket
gcloud storage ls gs://${BUCKET_NAME}
```

### Erro: "Bucket already exists"
```bash
# Listar buckets existentes
gcloud storage buckets list

# Usar bucket existente ou escolher outro nome
# Nomes de buckets são globalmente únicos no GCP
```

### Erro: "Invalid location"
```bash
# Verificar regiões disponíveis
gcloud storage locations list

# Regiões recomendadas no Brasil:
# - southamerica-east1 (São Paulo)
# - southamerica-west1 (Santiago) - mais próxima
```

### Erro: "Service Account não tem permissões"
```bash
# Verificar roles da Service Account
gcloud projects get-iam-policy ${PROJECT_ID} \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:${SA_EMAIL}"

# Adicionar role necessária
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/storage.objectAdmin"
```

---

## Resumo - Setup Rápido (southamerica-east1)

```bash
# Definir variáveis
PROJECT_ID="seu-project-id"
BUCKET_NAME="brewer-fotos"
REGION="southamerica-east1"
SA_NAME="brewer-storage"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

# 1. Criar bucket
gcloud storage buckets create gs://${BUCKET_NAME} \
  --project=${PROJECT_ID} \
  --location=${REGION} \
  --uniform-bucket-level-access

# 2. Criar Service Account
gcloud iam service-accounts create ${SA_NAME} \
  --display-name="Brewer Storage Service Account" \
  --project=${PROJECT_ID}

# 3. Dar permissões ao bucket
gcloud storage buckets add-iam-policy-binding gs://${BUCKET_NAME} \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/storage.objectAdmin"

# 4. Criar chave JSON (SALVAR EM LOCAL SEGURO!)
gcloud iam service-accounts keys create brewer-storage-key.json \
  --iam-account=${SA_EMAIL} \
  --project=${PROJECT_ID}

# 5. Configurar variáveis de ambiente e iniciar aplicação
export GCP_PROJECT_ID=${PROJECT_ID}
export GCP_STORAGE_BUCKET=${BUCKET_NAME}
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/brewer-storage-key.json
export SPRING_PROFILES_ACTIVE=prod-gcp

# 6. Testar
echo "test" > test.txt
gcloud storage cp test.txt gs://${BUCKET_NAME}/test.txt
gcloud storage ls gs://${BUCKET_NAME}/
gcloud storage rm gs://${BUCKET_NAME}/test.txt
rm test.txt
```

---

## Workload Identity (Recomendado para GKE)

Para produção no GKE, é recomendado usar **Workload Identity** ao invés de chaves JSON:

```bash
# 1. Habilitar Workload Identity no cluster (se ainda não estiver)
gcloud container clusters update ${CLUSTER_NAME} \
  --workload-pool=${PROJECT_ID}.svc.id.goog

# 2. Criar Kubernetes Service Account
kubectl create serviceaccount brewer-ksa --namespace=brewer

# 3. Vincular KSA com GSA
gcloud iam service-accounts add-iam-policy-binding ${SA_EMAIL} \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:${PROJECT_ID}.svc.id.goog[brewer/brewer-ksa]"

# 4. Anotar o KSA
kubectl annotate serviceaccount brewer-ksa \
  --namespace=brewer \
  iam.gke.io/gcp-service-account=${SA_EMAIL}

# 5. Usar no deployment
# spec:
#   serviceAccountName: brewer-ksa
```

**Benefícios do Workload Identity:**
- ✅ Sem necessidade de chaves JSON
- ✅ Rotação automática de credenciais
- ✅ Mais seguro
- ✅ Auditoria melhor

---

## Comparação: AWS S3 vs GCP Cloud Storage

| Feature | AWS S3 | GCP Cloud Storage |
|---------|--------|-------------------|
| **Comando CLI** | `aws s3` | `gcloud storage` |
| **Região Brasil** | sa-east-1 | southamerica-east1 |
| **Credenciais** | Access Key/Secret | Service Account JSON |
| **URL pública** | `s3.amazonaws.com/bucket/file` | `storage.googleapis.com/bucket/file` |
| **Uniformidade de acesso** | Block Public Access | Uniform Bucket Level Access |
| **Autenticação K8s** | IAM Role (IRSA) | Workload Identity |
| **Custo armazenamento** | $0.023/GB/mês | $0.020/GB/mês ✅ |

---

📝 **Nota**: Este documento assume que você já tem gcloud CLI configurado e um projeto GCP criado.

## Referências

- [Cloud Storage Documentation](https://cloud.google.com/storage/docs)
- [Service Accounts](https://cloud.google.com/iam/docs/service-accounts)
- [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity)
- [Cloud Storage Pricing](https://cloud.google.com/storage/pricing)