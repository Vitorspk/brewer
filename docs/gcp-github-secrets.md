# GitHub Actions Secrets para Deploy no GCP

Este documento lista todos os secrets necessários para configurar o deploy automático no Google Cloud Platform (GCP) usando GitHub Actions.

## Secrets Necessários

### 1. GCP_PROJECT_ID
**Descrição:** ID do projeto no Google Cloud Platform
**Como obter:**
```bash
gcloud projects list
```
**Exemplo:** `my-project-12345`

**Como adicionar no GitHub:**
- Vá em: Settings → Secrets and variables → Actions → New repository secret
- Name: `GCP_PROJECT_ID`
- Value: `[seu-project-id]`

---

### 2. GCP_SA_KEY
**Descrição:** Chave JSON da Service Account do GCP com permissões necessárias
**Como obter:**

#### Passo 1: Criar a Service Account
```bash
# Definir variáveis
PROJECT_ID="seu-project-id"
SA_NAME="github-actions-deployer"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

# Criar a service account
gcloud iam service-accounts create ${SA_NAME} \
  --display-name="GitHub Actions Deployer" \
  --description="Service account for GitHub Actions to deploy to GKE" \
  --project=${PROJECT_ID}
```

#### Passo 2: Atribuir as permissões necessárias
```bash
# Permissões para Artifact Registry (push de imagens Docker)
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/artifactregistry.writer"

# Permissões para GKE (gerenciar clusters e fazer deploy)
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/container.developer"

# Permissões para visualizar recursos do projeto
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/viewer"

# Permissões para criar e gerenciar service accounts (se necessário)
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/iam.serviceAccountUser"
```

#### Passo 3: Criar e baixar a chave JSON
```bash
# Criar a chave e salvar em arquivo
gcloud iam service-accounts keys create github-actions-key.json \
  --iam-account=${SA_EMAIL} \
  --project=${PROJECT_ID}

# Ver o conteúdo da chave (copie todo o conteúdo)
cat github-actions-key.json
```

**⚠️ IMPORTANTE:**
- Guarde esta chave de forma segura
- Nunca commite esta chave no repositório
- Após adicionar no GitHub Secrets, delete o arquivo local:
```bash
rm github-actions-key.json
```

**Como adicionar no GitHub:**
- Vá em: Settings → Secrets and variables → Actions → New repository secret
- Name: `GCP_SA_KEY`
- Value: Cole o conteúdo completo do arquivo JSON (incluindo as chaves `{}`)

---

### 3. TEST_DB_URL_GCP (Opcional - para testes)
**Descrição:** URL de conexão com o banco de dados para testes no GCP
**Exemplo:** `jdbc:mysql://localhost:3307/brewer_test`

**Como adicionar no GitHub:**
- Name: `TEST_DB_URL_GCP`
- Value: `[sua-url-de-teste]`

---

### 4. TEST_DB_USER_GCP (Opcional - para testes)
**Descrição:** Usuário do banco de dados para testes no GCP
**Exemplo:** `test_user`

**Como adicionar no GitHub:**
- Name: `TEST_DB_USER_GCP`
- Value: `[seu-usuario]`

---

### 5. TEST_DB_PASSWORD_GCP (Opcional - para testes)
**Descrição:** Senha do banco de dados para testes no GCP
**Exemplo:** `test_password`

**Como adicionar no GitHub:**
- Name: `TEST_DB_PASSWORD_GCP`
- Value: `[sua-senha]`

---

## Resumo dos Secrets Obrigatórios

Para o deploy no GCP funcionar, você **DEVE** configurar:

1. ✅ **GCP_PROJECT_ID** - ID do projeto GCP
2. ✅ **GCP_SA_KEY** - Chave JSON da Service Account

Os secrets de teste (TEST_DB_*_GCP) são opcionais, mas recomendados se você quiser executar testes durante o CI/CD. Note que eles têm o sufixo `_GCP` para diferenciá-los dos secrets usados para AWS.

---

## Pré-requisitos no GCP

Antes de executar o workflow, certifique-se de que você tem:

### 1. Artifact Registry configurado
```bash
# Criar repositório no Artifact Registry
gcloud artifacts repositories create brewer \
  --repository-format=docker \
  --location=southamerica-east1 \
  --description="Docker repository for Brewer application" \
  --project=${PROJECT_ID}
```

### 2. GKE Cluster criado
```bash
# Exemplo de criação de cluster GKE
gcloud container clusters create gke-dev \
  --region=southamerica-east1 \
  --num-nodes=2 \
  --machine-type=e2-standard-2 \
  --enable-autoscaling \
  --min-nodes=1 \
  --max-nodes=5 \
  --enable-autorepair \
  --enable-autoupgrade \
  --project=${PROJECT_ID}
```

### 3. Secrets do Kubernetes criados no cluster
```bash
# Conectar ao cluster
gcloud container clusters get-credentials gke-dev \
  --region=southamerica-east1 \
  --project=${PROJECT_ID}

# Criar namespace
kubectl create namespace brewer

# Criar secret com credenciais do banco de dados
kubectl create secret generic brewer-secrets \
  --from-literal=DATABASE_URL='jdbc:mysql://YOUR_DB_HOST:3306/brewer' \
  --from-literal=DATABASE_USERNAME='brewer_user' \
  --from-literal=DATABASE_PASSWORD='your_secure_password' \
  --namespace=brewer
```

---

## Configuração das Variáveis de Ambiente no Workflow

As seguintes variáveis estão configuradas no workflow e podem ser ajustadas conforme necessário:

```yaml
env:
  GCP_REGION: southamerica-east1        # Região do GCP
  GCP_PROJECT_ID: ${{ secrets.GCP_PROJECT_ID }}
  GKE_CLUSTER: gke-dev                  # Nome do cluster GKE
  GAR_REPOSITORY: brewer                # Nome do repositório no Artifact Registry
  K8S_NAMESPACE: brewer                 # Namespace do Kubernetes
```

Se você usar nomes diferentes para cluster, repositório ou namespace, atualize essas variáveis no arquivo [.github/workflows/deploy-to-gke.yml](.github/workflows/deploy-to-gke.yml).

---

## Como escolher entre AWS e GCP

Para escolher qual cloud usar para o deploy:

### Opção 1: Usando branches diferentes
- Branch `main` → Deploy para AWS (workflow: deploy-to-eks.yml)
- Criar branch `main-gcp` → Deploy para GCP (workflow: deploy-to-gke.yml)

### Opção 2: Ajustar os triggers dos workflows
Edite os arquivos de workflow para usar branches específicas:

**deploy-to-eks.yml** (AWS):
```yaml
on:
  push:
    branches:
      - main-aws  # Deploy somente quando push para essa branch
```

**deploy-to-gke.yml** (GCP):
```yaml
on:
  push:
    branches:
      - main-gcp  # Deploy somente quando push para essa branch
```

### Opção 3: Deploy manual via workflow_dispatch
Ambos os workflows suportam execução manual:
1. Vá em: Actions → Selecione o workflow desejado
2. Clique em "Run workflow"
3. Escolha a branch e o environment

---

## Testando a Configuração

Após adicionar todos os secrets, você pode testar executando manualmente o workflow:

1. Vá em: **Actions** → **Deploy to GKE**
2. Clique em: **Run workflow**
3. Selecione a branch: `feature/gcp-deployment`
4. Escolha o environment: `production` ou `staging`
5. Clique em: **Run workflow**

---

## Troubleshooting

### Erro: "Permission denied" no Artifact Registry
- Verifique se a Service Account tem a role `roles/artifactregistry.writer`
- Verifique se o repositório do Artifact Registry foi criado

### Erro: "Cluster not found"
- Verifique se o nome do cluster está correto no workflow
- Verifique se a região está correta
- Verifique se a Service Account tem a role `roles/container.developer`

### Erro: "Secret 'brewer-secrets' not found"
- Crie o secret no cluster Kubernetes antes de executar o workflow
- Verifique se o namespace está correto

---

## Segurança

### Boas Práticas:
1. ✅ Use Service Accounts com permissões mínimas necessárias
2. ✅ Nunca commite credenciais no código
3. ✅ Rotacione as chaves da Service Account periodicamente
4. ✅ Use secrets do Kubernetes para dados sensíveis
5. ✅ Habilite auditoria de logs no GCP
6. ✅ Use Workload Identity quando possível (mais seguro que Service Account keys)

### Rotação de Chaves:
```bash
# Criar nova chave
gcloud iam service-accounts keys create new-key.json \
  --iam-account=${SA_EMAIL}

# Atualizar no GitHub Secrets

# Deletar chave antiga
gcloud iam service-accounts keys delete KEY_ID \
  --iam-account=${SA_EMAIL}
```

---

## Referências

- [Google Cloud IAM Roles](https://cloud.google.com/iam/docs/understanding-roles)
- [Artifact Registry Documentation](https://cloud.google.com/artifact-registry/docs)
- [GKE Documentation](https://cloud.google.com/kubernetes-engine/docs)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)