# 🔄 Guia de Migração de Secrets

Este documento explica como migrar os secrets antigos para a nova convenção de nomenclatura com sufixos `_AWS` e `_GCP`.

## 📋 O que mudou?

### Antes (Antigo)
```yaml
# Secrets sem sufixo eram usados para AWS
TEST_DB_URL
TEST_DB_USER
TEST_DB_PASSWORD
```

### Agora (Novo)
```yaml
# Secrets AWS com sufixo _AWS
TEST_DB_URL_AWS
TEST_DB_USER_AWS
TEST_DB_PASSWORD_AWS

# Secrets GCP com sufixo _GCP
TEST_DB_URL_GCP
TEST_DB_USER_GCP
TEST_DB_PASSWORD_GCP
```

## 🎯 Por que mudamos?

A nova convenção torna **imediatamente claro** qual secret pertence a qual cloud provider:
- ✅ Fácil de identificar visualmente
- ✅ Evita confusão ao configurar secrets
- ✅ Permite usar ambas as clouds simultaneamente
- ✅ Padrão consistente: tudo com sufixo `_AWS` ou `_GCP`

## 📊 Tabela de Migração

| Secret Antigo | Secret Novo AWS | Secret Novo GCP | Status |
|---------------|-----------------|-----------------|--------|
| `TEST_DB_URL` | `TEST_DB_URL_AWS` | `TEST_DB_URL_GCP` | ⚠️ Migrar |
| `TEST_DB_USER` | `TEST_DB_USER_AWS` | `TEST_DB_USER_GCP` | ⚠️ Migrar |
| `TEST_DB_PASSWORD` | `TEST_DB_PASSWORD_AWS` | `TEST_DB_PASSWORD_GCP` | ⚠️ Migrar |
| `AWS_ACCESS_KEY_ID` | `AWS_ACCESS_KEY_ID` | N/A | ✅ Sem mudança |
| `AWS_SECRET_ACCESS_KEY` | `AWS_SECRET_ACCESS_KEY` | N/A | ✅ Sem mudança |

## 🔧 Como Migrar

### Passo 1: Verificar secrets atuais

1. Vá em: **GitHub Repository** → **Settings** → **Secrets and variables** → **Actions**
2. Anote os valores dos secrets que você tem configurados:
   - `TEST_DB_URL`
   - `TEST_DB_USER`
   - `TEST_DB_PASSWORD`

### Passo 2: Criar novos secrets

#### Se você usa AWS:
```bash
# No GitHub: Settings → Secrets → Actions → New repository secret

# Copie os valores dos secrets antigos para os novos:
Name: TEST_DB_URL_AWS
Value: [copie o valor de TEST_DB_URL]

Name: TEST_DB_USER_AWS
Value: [copie o valor de TEST_DB_USER]

Name: TEST_DB_PASSWORD_AWS
Value: [copie o valor de TEST_DB_PASSWORD]
```

#### Se você usa GCP:
```bash
# No GitHub: Settings → Secrets → Actions → New repository secret

Name: TEST_DB_URL_GCP
Value: [url do banco de teste GCP]

Name: TEST_DB_USER_GCP
Value: [usuário do banco de teste GCP]

Name: TEST_DB_PASSWORD_GCP
Value: [senha do banco de teste GCP]
```

#### Se você usa AMBOS (AWS e GCP):
```bash
# Crie os secrets para ambas as clouds:

# AWS
Name: TEST_DB_URL_AWS
Value: [url do banco de teste AWS]

Name: TEST_DB_USER_AWS
Value: [usuário do banco de teste AWS]

Name: TEST_DB_PASSWORD_AWS
Value: [senha do banco de teste AWS]

# GCP
Name: TEST_DB_URL_GCP
Value: [url do banco de teste GCP]

Name: TEST_DB_USER_GCP
Value: [usuário do banco de teste GCP]

Name: TEST_DB_PASSWORD_GCP
Value: [senha do banco de teste GCP]
```

### Passo 3: Atualizar os workflows

Os workflows já foram atualizados nesta branch (`feature/gcp-deployment`). Quando você fizer merge para `main`, os workflows começarão a usar os novos nomes.

### Passo 4: Deletar secrets antigos (opcional)

Após confirmar que tudo está funcionando com os novos secrets:

1. Vá em: **GitHub Repository** → **Settings** → **Secrets and variables** → **Actions**
2. Delete os secrets antigos:
   - ❌ `TEST_DB_URL` (se não estiver sendo usado por outros workflows)
   - ❌ `TEST_DB_USER` (se não estiver sendo usado por outros workflows)
   - ❌ `TEST_DB_PASSWORD` (se não estiver sendo usado por outros workflows)

## ⚡ Migração Rápida (Checklist)

### Para usuários AWS:
- [x] Anotar valores dos secrets antigos (`TEST_DB_*`)
- [x] Criar `TEST_DB_URL_AWS` com o valor de `TEST_DB_URL`
- [x] Criar `TEST_DB_USER_AWS` com o valor de `TEST_DB_USER`
- [x] Criar `TEST_DB_PASSWORD_AWS` com o valor de `TEST_DB_PASSWORD`
- [ ] Fazer merge da branch `feature/gcp-deployment` para `main`
- [ ] Testar o deploy no AWS
- [ ] Deletar secrets antigos após confirmar que tudo funciona

### Para usuários GCP:
- [x] Criar `GCP_PROJECT_ID` com o ID do projeto GCP
- [x] Criar `GCP_SA_KEY` com a chave JSON da Service Account
- [x] Criar `TEST_DB_URL_GCP` com a URL do banco de teste
- [x] Criar `TEST_DB_USER_GCP` com o usuário do banco de teste
- [x] Criar `TEST_DB_PASSWORD_GCP` com a senha do banco de teste
- [ ] Fazer merge da branch `feature/gcp-deployment` para `main`
- [ ] Testar o deploy no GCP

### Para usuários de AMBOS (AWS e GCP):
- [x] Anotar valores dos secrets AWS antigos
- [x] Criar todos os secrets `*_AWS`
- [x] Criar todos os secrets `*_GCP`
- [ ] Fazer merge da branch `feature/gcp-deployment` para `main`
- [ ] Testar deploy no AWS
- [ ] Testar deploy no GCP
- [ ] Deletar secrets antigos após confirmar que tudo funciona

## 🚨 Pontos de Atenção

### ⚠️ IMPORTANTE: Não delete os secrets antigos antes de fazer merge!

Se você deletar os secrets antigos (`TEST_DB_*`) antes de fazer merge da branch `feature/gcp-deployment`, os workflows na branch `main` vão falhar, pois ainda estão usando os nomes antigos.

**Ordem correta:**
1. ✅ Criar novos secrets (`*_AWS`, `*_GCP`)
2. ✅ Fazer merge da branch para `main`
3. ✅ Testar os workflows
4. ✅ Deletar secrets antigos (se não estiverem sendo usados)

### 🔍 Verificação

Para verificar se a migração foi bem-sucedida:

1. Execute o workflow manualmente:
   - **AWS**: Actions → Deploy to EKS → Run workflow
   - **GCP**: Actions → Deploy to GKE → Run workflow

2. Verifique os logs:
   - O step "Run tests" deve executar sem erros
   - Se falhar, verifique se os secrets foram criados corretamente

## 💡 Dicas

1. **Faça backup dos valores**: Antes de deletar secrets antigos, anote os valores em um local seguro (não no código!)

2. **Teste primeiro no workflow_dispatch**: Use a execução manual dos workflows para testar antes de fazer push para `main`

3. **Migração gradual**: Você pode manter os secrets antigos por um tempo enquanto valida os novos

4. **Documentação**: Mantenha este guia acessível para futuros desenvolvedores do time

## 📞 Problemas Comuns

### Erro: "TEST_DB_URL_AWS not found"
**Solução**: Você esqueceu de criar o secret `TEST_DB_URL_AWS`. Vá em Settings → Secrets → Actions e crie o secret.

### Erro: Workflows ainda usam nomes antigos
**Solução**: Você precisa fazer merge da branch `feature/gcp-deployment` para `main` primeiro.

### Erro: Tests falham com credenciais inválidas
**Solução**: Verifique se copiou os valores corretos dos secrets antigos para os novos.

## 📚 Documentação Relacionada

- [Comparação de Secrets AWS vs GCP](./SECRETS_COMPARISON.md)
- [Documentação completa GCP](./gcp-github-secrets.md)
- [README principal](../README.md)

## ✅ Conclusão

Após seguir este guia, você terá:
- ✅ Secrets organizados com nomenclatura clara
- ✅ Suporte para deploy em AWS e GCP simultaneamente
- ✅ Workflows atualizados e funcionando
- ✅ Melhor manutenibilidade do projeto

Se tiver dúvidas ou problemas durante a migração, consulte a documentação ou abra uma issue no repositório.