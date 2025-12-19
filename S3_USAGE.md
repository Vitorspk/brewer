# Como Usar AWS S3 Storage - Brewer

## 🎯 Visão Geral

O sistema Brewer suporta dois modos de armazenamento de fotos:

| Modo | Profile | Storage | Quando usar |
|------|---------|---------|-------------|
| **Desenvolvimento** | `!prod` (padrão) | Local (filesystem) | Desenvolvimento local, testes |
| **Produção** | `prod` | AWS S3 | Deploy em servidor, alta disponibilidade |

## ✅ Recursos AWS Criados

### Bucket S3
- **Nome**: `brewer-fotos`
- **Região**: `sa-east-1` (São Paulo, Brasil)
- **Configurações**:
  - ✅ Public Access Block habilitado
  - ✅ CORS configurado para localhost:8080
  - ✅ Versionamento: Desabilitado

### IAM User
- **Username**: `brewer-s3-user`
- **Política**: `BrewerS3FullAccessPolicy`
- **Permissões**: Full S3 access (s3:*)

### Credenciais
As credenciais AWS foram criadas e estão disponíveis localmente em:
- `/tmp/brewer-s3-credentials.txt` (se ainda existir)

**⚠️ IMPORTANTE**: As credenciais NÃO estão commitadas no repositório por segurança.
Para obter as credenciais, execute:
```bash
aws iam list-access-keys --user-name brewer-s3-user
```

## 🚀 Quick Start

### 1. Configurar Variáveis de Ambiente

```bash
# Obter credenciais do IAM user
export AWS_ACCESS_KEY_ID=$(aws iam list-access-keys --user-name brewer-s3-user --query 'AccessKeyMetadata[0].AccessKeyId' --output text)

# Nota: SecretAccessKey precisa ser recuperado do arquivo local onde foi salvo
# ou criar uma nova access key:
# aws iam create-access-key --user-name brewer-s3-user

export AWS_SECRET_ACCESS_KEY=sua-secret-key-aqui
export AWS_REGION=sa-east-1
export AWS_S3_BUCKET=brewer-fotos
export SPRING_PROFILES_ACTIVE=prod
```

### 2. Iniciar Aplicação

```bash
mvn spring-boot:run
```

## 🔄 Alternando Entre Modos

### Modo Local (Desenvolvimento - Padrão)

```bash
# NÃO definir SPRING_PROFILES_ACTIVE ou definir como default
unset SPRING_PROFILES_ACTIVE

# Iniciar aplicação
mvn spring-boot:run

# Fotos salvas em: ~/.brewerfotos/
```

### Modo S3 (Produção)

```bash
# Definir profile prod
export SPRING_PROFILES_ACTIVE=prod

# Configurar credenciais AWS (usar suas próprias credenciais)
export AWS_ACCESS_KEY_ID=<sua-access-key>
export AWS_SECRET_ACCESS_KEY=<sua-secret-key>
export AWS_REGION=sa-east-1
export AWS_S3_BUCKET=brewer-fotos

# Iniciar aplicação
mvn spring-boot:run

# Fotos salvas em: s3://brewer-fotos/
```

## 🧪 Testando S3

### 1. Verificar Acesso ao Bucket

```bash
# Configurar credenciais
export AWS_ACCESS_KEY_ID=<sua-access-key>
export AWS_SECRET_ACCESS_KEY=<sua-secret-key>

# Listar bucket
aws s3 ls s3://brewer-fotos/
```

### 2. Upload Manual de Teste

```bash
# Criar imagem de teste
echo "Test image" > /tmp/test-cerveja.jpg

# Upload
aws s3 cp /tmp/test-cerveja.jpg s3://brewer-fotos/test-cerveja.jpg

# Verificar
aws s3 ls s3://brewer-fotos/ | grep test-cerveja

# Deletar
aws s3 rm s3://brewer-fotos/test-cerveja.jpg
```

### 3. Testar via Aplicação

```bash
# 1. Iniciar aplicação em modo S3
export SPRING_PROFILES_ACTIVE=prod
export AWS_ACCESS_KEY_ID=<sua-access-key>
export AWS_SECRET_ACCESS_KEY=<sua-secret-key>
export AWS_REGION=sa-east-1
mvn spring-boot:run

# 2. Acessar: http://localhost:8080/cervejas/novo

# 3. Fazer upload de uma foto

# 4. Verificar no S3
aws s3 ls s3://brewer-fotos/
```

## 🔍 Monitoramento

### Ver Fotos no Bucket

```bash
# Listar todas as fotos
aws s3 ls s3://brewer-fotos/ --recursive

# Ver tamanho total
aws s3 ls s3://brewer-fotos/ --recursive --human-readable --summarize
```

### Logs da Aplicação

```bash
# Log mostrará qual storage está sendo usado
tail -f target/spring-boot.log | grep -i "foto\|storage"

# Você verá algo como:
# FotoStorageS3 : Salvando foto no S3: bucket=brewer-fotos, key=abc123_cerveja.jpg
```

## 🛠️ Troubleshooting

### Erro: "Access Denied"

**Causa**: Credenciais inválidas ou sem permissão

**Solução**:
```bash
# Verificar se as credenciais estão corretas
aws sts get-caller-identity

# Deve retornar informações do usuário brewer-s3-user
```

### Erro: "Bucket does not exist"

**Causa**: Bucket não existe ou região incorreta

**Solução**:
```bash
# Verificar se bucket existe
aws s3 ls s3://brewer-fotos/ --region sa-east-1

# Se não existir, criar:
aws s3 mb s3://brewer-fotos --region sa-east-1
```

### Aplicação usando storage local em vez de S3

**Causa**: Profile prod não está ativo

**Solução**:
```bash
# Verificar profile ativo
echo $SPRING_PROFILES_ACTIVE

# Deve mostrar: prod

# Se não mostrar, definir:
export SPRING_PROFILES_ACTIVE=prod

# Reiniciar aplicação
```

## 📊 Custos

### Estimativa Mensal (sa-east-1)

| Recurso | Quantidade | Custo Unitário | Total |
|---------|------------|----------------|-------|
| Armazenamento | 5 GB | $0.023/GB/mês | $0.12 |
| Requisições PUT | 1.000 | $0.005/1000 | $0.005 |
| Requisições GET | 10.000 | $0.0004/1000 | $0.004 |
| Transferência | 1 GB | Grátis | $0.00 |
| **TOTAL** | | | **~$0.13/mês** |

### Como Reduzir Custos

1. **Lifecycle policies**: Deletar fotos antigas automaticamente
2. **Compression**: Comprimir imagens antes do upload
3. **CloudFront**: Cache de imagens (se muito tráfego)
4. **S3 Intelligent-Tiering**: Para arquivos raramente acessados

## 🔐 Segurança

### ⚠️ IMPORTANTE

- **NUNCA** commite credenciais no Git
- Use AWS Secrets Manager em produção
- Rotacione access keys periodicamente
- Habilite MFA no usuário IAM
- Use IAM Roles em EC2/ECS (melhor que access keys)

### Configuração Segura para Produção

```bash
# 1. Armazenar credenciais em arquivo .env (não commitado)
cat > .env <<EOF
export AWS_ACCESS_KEY_ID=<sua-access-key>
export AWS_SECRET_ACCESS_KEY=<sua-secret-key>
export AWS_REGION=sa-east-1
export AWS_S3_BUCKET=brewer-fotos
EOF

# 2. Adicionar .env ao .gitignore
echo ".env" >> .gitignore

# 3. Carregar variáveis
source .env

# 4. Iniciar aplicação
mvn spring-boot:run
```

## 📚 Arquivos Relacionados

- [AWS_S3_SETUP.md](AWS_S3_SETUP.md) - Guia completo de setup com AWS CLI
- [src/main/resources/application.properties](src/main/resources/application.properties) - Configurações da aplicação

## 🔄 Recuperar ou Criar Novas Credenciais

### Listar Access Keys Existentes

```bash
aws iam list-access-keys --user-name brewer-s3-user
```

### Criar Nova Access Key

```bash
aws iam create-access-key --user-name brewer-s3-user

# Salvar output em local seguro!
```

### Rotacionar Credenciais (Recomendado)

```bash
# 1. Criar nova key
aws iam create-access-key --user-name brewer-s3-user

# 2. Testar nova key
export AWS_ACCESS_KEY_ID=<nova-key>
export AWS_SECRET_ACCESS_KEY=<novo-secret>
aws s3 ls s3://brewer-fotos/

# 3. Se funcionar, deletar key antiga
aws iam delete-access-key --user-name brewer-s3-user --access-key-id <key-antiga>
```

## 🆘 Suporte

Em caso de dúvidas:
1. Verificar logs da aplicação
2. Testar AWS CLI manualmente
3. Verificar permissões IAM
4. Consultar documentação AWS S3

---

📝 Documentação atualizada em: 2025-12-19
