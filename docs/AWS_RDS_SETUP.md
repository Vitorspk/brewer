# AWS RDS MySQL - Configuração do Banco de Dados Brewer

## ⚠️ AVISO DE SEGURANÇA CRÍTICO

**SE VOCÊ ESTÁ VENDO CREDENCIAIS EXPOSTAS NESTE ARQUIVO:**
1. As credenciais foram comprometidas e devem ser rotacionadas IMEDIATAMENTE
2. Nunca commite credenciais ou endpoints de produção no repositório
3. Use variáveis de ambiente e arquivos .env (não versionados)

## 📊 Informações da Instância RDS

A aplicação Brewer pode utilizar um banco de dados MySQL hospedado na AWS RDS.

### Detalhes Recomendados da Instância

- **Instance Class**: db.t3.micro (1 vCPU, 1GB RAM) - Free Tier elegível
- **Storage**: 20GB gp3 (3000 IOPS, 125 MB/s throughput)
- **Engine**: MySQL 8.0.40+
- **Região**: sa-east-1 (São Paulo) ou us-east-1
- **Publicly Accessible**: **NÃO** (use VPN/Bastion para acesso externo)
- **Multi-AZ**: Recomendado para produção
- **Backup Retention**: 7-30 dias

## 🔐 Credenciais (NUNCA COMMITE ESTAS INFORMAÇÕES)

As credenciais devem estar **SOMENTE** no arquivo `.env.rds` (listado em .gitignore):

```bash
# .env.rds - NUNCA COMMITE ESTE ARQUIVO
RDS_HOST=your-rds-endpoint.region.rds.amazonaws.com
RDS_PORT=3306
RDS_USER=admin
RDS_PASSWORD=your-secure-password-here
RDS_DATABASE=brewer
RDS_TEST_DATABASE=brewer_test
```

### Rotação de Credenciais

Se credenciais foram expostas:

```bash
# 1. Conectar ao RDS via AWS Console ou CLI
aws rds modify-db-instance \
  --db-instance-identifier brewer-db \
  --master-user-password "NEW_SECURE_PASSWORD" \
  --apply-immediately

# 2. Aguardar aplicação (pode levar alguns minutos)
aws rds describe-db-instances \
  --db-instance-identifier brewer-db \
  --query 'DBInstances[0].DBInstanceStatus'

# 3. Atualizar .env.rds local
# 4. Atualizar GitHub Actions Secrets
# 5. Atualizar variáveis de ambiente em produção
```

## 🚀 Uso

### 1. Criar Instância RDS

```bash
# Criar security group
aws ec2 create-security-group \
  --group-name brewer-rds-sg \
  --description "Security group for Brewer RDS MySQL" \
  --vpc-id vpc-xxxxx

# Adicionar regra APENAS para seu IP (NUNCA use 0.0.0.0/0)
YOUR_IP=$(curl -s https://api.ipify.org)
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 3306 \
  --cidr ${YOUR_IP}/32

# Criar subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name brewer-subnet-group \
  --db-subnet-group-description "Subnet group for Brewer DB" \
  --subnet-ids subnet-xxxxx subnet-yyyyy

# Criar instância RDS
aws rds create-db-instance \
  --db-instance-identifier brewer-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0.40 \
  --master-username admin \
  --master-user-password "SECURE_PASSWORD_HERE" \
  --allocated-storage 20 \
  --storage-type gp3 \
  --vpc-security-group-ids sg-xxxxx \
  --db-subnet-group-name brewer-subnet-group \
  --backup-retention-period 7 \
  --no-publicly-accessible \
  --db-name brewer
```

### 2. Criar Banco de Testes

Após carregar variáveis de ambiente do `.env.rds`:

```bash
# Carregar variáveis
source .env.rds

# Criar banco de testes
./scripts/setup-rds-test-db.sh
```

### 3. Configurar Aplicação

#### Local Development (.env)

```bash
# .env - para desenvolvimento local
DATABASE_URL=jdbc:mysql://localhost:3306/brewer?allowPublicKeyRetrieval=true&useSSL=false
DATABASE_USERNAME=brewer
DATABASE_PASSWORD=brewer_password
```

#### Production (.env.rds)

```bash
# .env.rds - para RDS (NÃO COMMITE)
DATABASE_URL=jdbc:mysql://${RDS_HOST}:${RDS_PORT}/${RDS_DATABASE}?allowPublicKeyRetrieval=true&useSSL=true&requireSSL=false
DATABASE_USERNAME=${RDS_USER}
DATABASE_PASSWORD=${RDS_PASSWORD}
```

#### Docker Compose

```bash
# Use local MySQL por padrão
docker-compose up

# Para usar RDS, crie .env com as credenciais RDS
cp .env.rds .env
docker-compose up
```

### 4. GitHub Actions

Configure os seguintes **Secrets** no repositório (Settings > Secrets and variables > Actions):

```
DATABASE_URL=jdbc:mysql://endpoint:3306/brewer?...
DATABASE_USERNAME=admin
DATABASE_PASSWORD=your-secure-password
TEST_DB_URL=jdbc:mysql://endpoint:3306/brewer_test?...
TEST_DB_USER=admin
TEST_DB_PASSWORD=your-secure-password
```

## 🔒 Segurança

### Checklist de Segurança

- [ ] RDS **não** está publicamente acessível
- [ ] Security Group permite acesso APENAS de IPs específicos
- [ ] Senha forte com 20+ caracteres (use gerenciador de senhas)
- [ ] Credenciais armazenadas em AWS Secrets Manager (recomendado)
- [ ] Backup automático habilitado
- [ ] Encryption at rest habilitada
- [ ] SSL/TLS enforced para conexões
- [ ] Logs de audit habilitados
- [ ] Rotação de senha a cada 90 dias

### Restringir Security Group

```bash
# Remover acesso público se existir
aws ec2 revoke-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 3306 \
  --cidr 0.0.0.0/0

# Adicionar APENAS seu IP
YOUR_IP=$(curl -s https://api.ipify.org)
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 3306 \
  --cidr ${YOUR_IP}/32
```

### Usar AWS Secrets Manager (Recomendado)

```bash
# Criar secret
aws secretsmanager create-secret \
  --name brewer/rds/credentials \
  --secret-string '{"username":"admin","password":"SECURE_PASSWORD"}'

# Atualizar aplicação para ler do Secrets Manager
# Adicionar AWS SDK dependency ao pom.xml
```

## 💰 Custos

### Free Tier (12 meses)
- 750 horas/mês de db.t3.micro
- 20GB de armazenamento gp3
- 20GB de backup

### Após Free Tier (estimativa mensal - sa-east-1)
- db.t3.micro: ~$15/mês
- Storage (20GB gp3): ~$3/mês
- Backup adicional (20GB): ~$2/mês
- **Total estimado: ~$20/mês**

## 🛠️ Troubleshooting

### Erro de Conexão

```bash
# Verificar status da instância
aws rds describe-db-instances \
  --db-instance-identifier brewer-db \
  --query 'DBInstances[0].[DBInstanceStatus,Endpoint.Address]'

# Testar conectividade
telnet your-endpoint.rds.amazonaws.com 3306

# Verificar security group
aws ec2 describe-security-groups \
  --group-ids sg-xxxxx
```

### Erros Comuns

1. **Connection refused**: Verificar security group permite seu IP
2. **Access denied**: Verificar username/password
3. **Unknown database**: Criar banco de testes via script
4. **SSL connection error**: Ajustar parâmetros de conexão

## 📚 Referências

- [AWS RDS MySQL Documentation](https://docs.aws.amazon.com/rds/latest/userguide/CHAP_MySQL.html)
- [AWS Security Best Practices](https://docs.aws.amazon.com/rds/latest/userguide/CHAP_BestPractices.Security.html)
- [MySQL 8.0 Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/)