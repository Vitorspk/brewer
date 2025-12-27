# Comparação: Secrets AWS vs GCP

Este documento mostra lado a lado os secrets necessários para cada cloud provider.

## 📊 Tabela Comparativa de Secrets

| Secret Name | AWS | GCP | Descrição |
|-------------|-----|-----|-----------|
| **Credenciais Cloud** |
| `AWS_ACCESS_KEY_ID` | ✅ | ❌ | Access Key ID da AWS IAM User |
| `AWS_SECRET_ACCESS_KEY` | ✅ | ❌ | Secret Access Key da AWS IAM User |
| `GCP_PROJECT_ID` | ❌ | ✅ | ID do projeto no Google Cloud |
| `GCP_SA_KEY` | ❌ | ✅ | Chave JSON da Service Account do GCP |
| **Secrets de Teste** |
| `TEST_DB_URL_AWS` | ✅ | ❌ | URL do banco de dados de teste (AWS) |
| `TEST_DB_USER_AWS` | ✅ | ❌ | Usuário do banco de teste (AWS) |
| `TEST_DB_PASSWORD_AWS` | ✅ | ❌ | Senha do banco de teste (AWS) |
| `TEST_DB_URL_GCP` | ❌ | ✅ | URL do banco de dados de teste (GCP) |
| `TEST_DB_USER_GCP` | ❌ | ✅ | Usuário do banco de teste (GCP) |
| `TEST_DB_PASSWORD_GCP` | ❌ | ✅ | Senha do banco de teste (GCP) |

## 🔐 Secrets Obrigatórios

### Para Deploy na AWS (EKS)
```yaml
Obrigatórios:
  ✅ AWS_ACCESS_KEY_ID
  ✅ AWS_SECRET_ACCESS_KEY

Opcionais (para testes):
  ⭕ TEST_DB_URL_AWS
  ⭕ TEST_DB_USER_AWS
  ⭕ TEST_DB_PASSWORD_AWS
```

### Para Deploy no GCP (GKE)
```yaml
Obrigatórios:
  ✅ GCP_PROJECT_ID
  ✅ GCP_SA_KEY

Opcionais (para testes):
  ⭕ TEST_DB_URL_GCP
  ⭕ TEST_DB_USER_GCP
  ⭕ TEST_DB_PASSWORD_GCP
```

## 📝 Como Identificar Rapidamente

### Convenção de Nomenclatura:
- **AWS**: Secrets começam com `AWS_` ou terminam com `_AWS`
- **GCP**: Secrets começam com `GCP_` ou terminam com `_GCP`

### Exemplos:
```bash
# AWS
AWS_ACCESS_KEY_ID          # ← Claramente AWS (prefixo AWS_)
TEST_DB_URL_AWS            # ← Usado no workflow AWS (sufixo _AWS)

# GCP
GCP_PROJECT_ID             # ← Claramente GCP (prefixo GCP_)
TEST_DB_URL_GCP            # ← Usado no workflow GCP (sufixo _GCP)
```

## 🚀 Quick Start

### 1️⃣ Configurar Secrets AWS
```bash
# No GitHub: Settings → Secrets → Actions → New repository secret

Name: AWS_ACCESS_KEY_ID
Value: [sua-access-key-id]

Name: AWS_SECRET_ACCESS_KEY
Value: [sua-secret-access-key]

# Opcionais (para testes)
Name: TEST_DB_URL_AWS
Value: [sua-url-de-teste]

Name: TEST_DB_USER_AWS
Value: [seu-usuario]

Name: TEST_DB_PASSWORD_AWS
Value: [sua-senha]
```

### 2️⃣ Configurar Secrets GCP
```bash
# No GitHub: Settings → Secrets → Actions → New repository secret

Name: GCP_PROJECT_ID
Value: [seu-project-id]

Name: GCP_SA_KEY
Value: [conteúdo-completo-do-json]

# Opcionais (para testes)
Name: TEST_DB_URL_GCP
Value: [sua-url-de-teste]

Name: TEST_DB_USER_GCP
Value: [seu-usuario]

Name: TEST_DB_PASSWORD_GCP
Value: [sua-senha]
```

## 🎯 Workflows

### AWS Workflow: `deploy-to-eks.yml`
```yaml
Usa os seguintes secrets:
  - AWS_ACCESS_KEY_ID
  - AWS_SECRET_ACCESS_KEY
  - TEST_DB_URL_AWS (opcional)
  - TEST_DB_USER_AWS (opcional)
  - TEST_DB_PASSWORD_AWS (opcional)

Trigger: push para branch 'main'
```

### GCP Workflow: `deploy-to-gke.yml`
```yaml
Usa os seguintes secrets:
  - GCP_PROJECT_ID
  - GCP_SA_KEY
  - TEST_DB_URL_GCP (opcional)
  - TEST_DB_USER_GCP (opcional)
  - TEST_DB_PASSWORD_GCP (opcional)

Trigger: push para branch 'main'
```

## ⚙️ Variáveis de Ambiente nos Workflows

### AWS (EKS)
```yaml
env:
  AWS_REGION: sa-east-1
  ECR_REPOSITORY: brewer
  EKS_CLUSTER: eks-dev
  K8S_NAMESPACE: brewer
```

### GCP (GKE)
```yaml
env:
  GCP_REGION: southamerica-east1
  GCP_PROJECT_ID: ${{ secrets.GCP_PROJECT_ID }}
  GKE_CLUSTER: gke-dev
  GAR_REPOSITORY: brewer
  K8S_NAMESPACE: brewer
```

## 🔍 Verificando seus Secrets

Para verificar se todos os secrets necessários estão configurados:

1. Vá em: **GitHub Repository** → **Settings** → **Secrets and variables** → **Actions**
2. Verifique se você tem os secrets listados para a cloud que deseja usar

### Checklist AWS ✅
- [ ] `AWS_ACCESS_KEY_ID`
- [ ] `AWS_SECRET_ACCESS_KEY`
- [ ] `TEST_DB_URL_AWS` (opcional)
- [ ] `TEST_DB_USER_AWS` (opcional)
- [ ] `TEST_DB_PASSWORD_AWS` (opcional)

### Checklist GCP ✅
- [ ] `GCP_PROJECT_ID`
- [ ] `GCP_SA_KEY`
- [ ] `TEST_DB_URL_GCP` (opcional)
- [ ] `TEST_DB_USER_GCP` (opcional)
- [ ] `TEST_DB_PASSWORD_GCP` (opcional)

## 📚 Documentação Detalhada

- AWS: Ver documentação em [README.md](../README.md)
- GCP: Ver documentação em [gcp-github-secrets.md](./gcp-github-secrets.md)

## 💡 Dicas

1. **Organize seus secrets**: A convenção de nomenclatura facilita identificar qual secret pertence a qual cloud
2. **Teste incrementalmente**: Configure primeiro os secrets obrigatórios, teste o deploy, depois adicione os opcionais
3. **Use workflow_dispatch**: Execute os workflows manualmente primeiro antes de ativar os triggers automáticos
4. **Segurança**: Nunca commite secrets no código. Sempre use GitHub Secrets
5. **Rotação**: Rotacione as credenciais periodicamente para manter a segurança