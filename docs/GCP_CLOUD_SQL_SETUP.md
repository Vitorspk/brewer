# GCP Cloud SQL MySQL - Configuração do Banco de Dados Brewer

## ⚠️ AVISO DE SEGURANÇA CRÍTICO

**SE VOCÊ ESTÁ VENDO CREDENCIAIS EXPOSTAS NESTE ARQUIVO:**
1. As credenciais foram comprometidas e devem ser rotacionadas IMEDIATAMENTE
2. Nunca commite credenciais ou endpoints de produção no repositório
3. Use variáveis de ambiente e arquivos .env (não versionados)

## 📊 Informações da Instância Cloud SQL

A aplicação Brewer pode utilizar um banco de dados MySQL hospedado no GCP Cloud SQL.

### Detalhes Recomendados da Instância

- **Machine Type**: db-f1-micro (1 vCPU compartilhada, 614MB RAM) - Equivalente ao db.t3.micro da AWS
- **Storage**: 20GB SSD com auto-resize habilitado
- **Engine**: MySQL 8.0+
- **Região**: southamerica-east1 (São Paulo)
- **Private IP**: **SIM** (sem IP público) - use Cloud SQL Proxy ou VPN para acesso externo
- **High Availability**: Recomendado para produção (adiciona standby em outra zona)
- **Backup**: Automático diário às 03:00 UTC
- **Maintenance Window**: Domingo às 04:00 UTC

## 🔐 Credenciais (NUNCA COMMITE ESTAS INFORMAÇÕES)

As credenciais devem estar **SOMENTE** no arquivo `.env.cloudsql` (listado em .gitignore):

```bash
# .env.cloudsql - NUNCA COMMITE ESTE ARQUIVO
CLOUDSQL_CONNECTION_NAME=vschiavo-home:southamerica-east1:brewer-db
CLOUDSQL_PRIVATE_IP=10.x.x.x
CLOUDSQL_USER=admin
CLOUDSQL_PASSWORD=your-secure-password-here
CLOUDSQL_DATABASE=brewer
CLOUDSQL_TEST_DATABASE=brewer_test
```

### Rotação de Credenciais

Se credenciais foram expostas:

```bash
# 1. Alterar senha do usuário admin
gcloud sql users set-password admin \
  --instance=brewer-db \
  --password="NEW_SECURE_PASSWORD"

# 2. Verificar status da instância
gcloud sql instances describe brewer-db \
  --format="value(state)"

# 3. Atualizar .env.cloudsql local
# 4. Atualizar GitHub Actions Secrets
# 5. Atualizar Kubernetes Secrets em produção
```

## 🚀 Uso

### 1. Habilitar APIs Necessárias

```bash
# Habilitar Cloud SQL Admin API
gcloud services enable sqladmin.googleapis.com

# Habilitar Service Networking API (para Private IP)
gcloud services enable servicenetworking.googleapis.com
```

### 2. Criar Instância Cloud SQL

```bash
# Definir variáveis
export PROJECT_ID="vschiavo-home"
export REGION="southamerica-east1"
export INSTANCE_NAME="brewer-db"
export ADMIN_PASSWORD="qopjof-biRde6-nymrib"  # MUDE ESTA SENHA!

# Criar instância (pode levar 5-10 minutos)
gcloud sql instances create ${INSTANCE_NAME} \
  --database-version=MYSQL_8_0 \
  --tier=db-f1-micro \
  --region=${REGION} \
  --storage-type=SSD \
  --storage-size=20GB \
  --storage-auto-increase \
  --backup-start-time=03:00 \
  --maintenance-window-day=SUN \
  --maintenance-window-hour=04 \
  --no-assign-ip \
  --network=projects/${PROJECT_ID}/global/networks/default

# Verificar criação
gcloud sql instances describe ${INSTANCE_NAME}
```

### 3. Criar Usuário Admin e Bancos de Dados

```bash
# Criar usuário admin
gcloud sql users create admin \
  --instance=${INSTANCE_NAME} \
  --password="${ADMIN_PASSWORD}"

# Criar banco de dados principal
gcloud sql databases create brewer \
  --instance=${INSTANCE_NAME}

# Criar banco de testes
gcloud sql databases create brewer_test \
  --instance=${INSTANCE_NAME}

# Listar bancos criados
gcloud sql databases list --instance=${INSTANCE_NAME}
```

### 4. Obter IP Privado

```bash
# Obter IP privado da instância
gcloud sql instances describe ${INSTANCE_NAME} \
  --format="value(ipAddresses[0].ipAddress)"

# Obter connection name (formato: PROJECT:REGION:INSTANCE)
gcloud sql instances describe ${INSTANCE_NAME} \
  --format="value(connectionName)"
```

### 5. Configurar Acesso via Cloud SQL Proxy

Para acessar de forma segura localmente:

```bash
# Baixar Cloud SQL Proxy (Linux/Mac)
curl -o cloud-sql-proxy https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.13.0/cloud-sql-proxy.linux.amd64
chmod +x cloud-sql-proxy

# Iniciar proxy (substitua CONNECTION_NAME)
./cloud-sql-proxy vschiavo-home:southamerica-east1:brewer-db

# Em outro terminal, conectar via MySQL client
mysql -h 127.0.0.1 -u admin -p brewer
```

### 6. Configurar Aplicação

#### Local Development (.env)

```bash
# .env - para desenvolvimento local via Cloud SQL Proxy
DATABASE_URL=jdbc:mysql://127.0.0.1:3306/brewer?allowPublicKeyRetrieval=true&useSSL=false
DATABASE_USERNAME=admin
DATABASE_PASSWORD=qopjof-biRde6-nymrib
```

#### Production GKE (.env.cloudsql)

```bash
# .env.cloudsql - para GKE com Private IP (NÃO COMMITE)
DATABASE_URL=jdbc:mysql://10.x.x.x:3306/brewer?useSSL=true&requireSSL=false&serverTimezone=UTC
DATABASE_USERNAME=admin
DATABASE_PASSWORD=qopjof-biRde6-nymrib
```

#### Kubernetes com Cloud SQL Proxy Sidecar

Para usar Cloud SQL Proxy como sidecar no GKE:

```yaml
# deployment.yaml
spec:
  containers:
  - name: brewer
    image: docker.io/vitorspk/brewer:latest
    env:
    - name: DATABASE_URL
      value: "jdbc:mysql://127.0.0.1:3306/brewer?useSSL=false"
    - name: DATABASE_USERNAME
      valueFrom:
        secretKeyRef:
          name: brewer-secrets
          key: DATABASE_USERNAME
    - name: DATABASE_PASSWORD
      valueFrom:
        secretKeyRef:
          name: brewer-secrets
          key: DATABASE_PASSWORD

  # Cloud SQL Proxy sidecar
  - name: cloud-sql-proxy
    image: gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.13.0
    args:
    - "--private-ip"
    - "vschiavo-home:southamerica-east1:brewer-db"
    securityContext:
      runAsNonRoot: true
```

### 7. GitHub Actions

Configure os seguintes **Secrets** no repositório (Settings > Secrets and variables > Actions):

```
DATABASE_URL=jdbc:mysql://10.x.x.x:3306/brewer?...
DATABASE_USERNAME=admin
DATABASE_PASSWORD=your-secure-password
TEST_DB_URL=jdbc:mysql://10.x.x.x:3306/brewer_test?...
TEST_DB_USER=admin
TEST_DB_PASSWORD=your-secure-password
GCP_CREDENTIALS_JSON=<service-account-json>
```

## 🔒 Segurança

### Checklist de Segurança

- [x] Cloud SQL **não** tem IP público (--no-assign-ip)
- [x] Acesso via Private IP ou Cloud SQL Proxy
- [x] Senha forte com 20+ caracteres (use gerenciador de senhas)
- [ ] Credenciais armazenadas em Secret Manager (recomendado)
- [x] Backup automático habilitado (diário às 03:00)
- [x] Encryption at rest habilitada (padrão no GCP)
- [x] SSL/TLS enforced para conexões
- [ ] Cloud SQL Insights habilitado (monitoramento)
- [ ] Rotação de senha a cada 90 dias
- [ ] Database flags configurados (max_connections, etc.)

### Restringir Acesso (Authorized Networks)

Se você optou por ter IP público (não recomendado):

```bash
# Remover acesso público se existir
gcloud sql instances patch ${INSTANCE_NAME} \
  --clear-authorized-networks

# Adicionar APENAS seu IP
YOUR_IP=$(curl -s https://api.ipify.org)
gcloud sql instances patch ${INSTANCE_NAME} \
  --authorized-networks=${YOUR_IP}/32
```

### Usar Secret Manager (Recomendado)

```bash
# Criar secret
gcloud secrets create brewer-db-password \
  --data-file=- <<EOF
qopjof-biRde6-nymrib
EOF

# Conceder acesso ao service account
gcloud secrets add-iam-policy-binding brewer-db-password \
  --member="serviceAccount:github-actions-terraform@vschiavo-home.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Ler secret via gcloud (para teste)
gcloud secrets versions access latest --secret="brewer-db-password"
```

### Habilitar Cloud SQL Insights

```bash
# Habilitar Query Insights para monitoramento de performance
gcloud sql instances patch ${INSTANCE_NAME} \
  --insights-config-query-insights-enabled
```

## 💰 Custos

### Always Free Tier (sem limite de tempo!)

O GCP oferece um tier gratuito PERMANENTE para Cloud SQL:
- **1x db-f1-micro** em regiões US (us-central1, us-west1, us-east1)
- 30GB HDD storage (ou 10GB SSD)
- 10GB backups

**NOTA**: `southamerica-east1` (São Paulo) **NÃO está no Free Tier**.

### Custos Estimados (southamerica-east1)

#### Configuração Básica (db-f1-micro)
- db-f1-micro: ~$8/mês (730 horas)
- Storage SSD (20GB): ~$4/mês
- Backup (20GB): ~$2/mês
- **Total estimado: ~$14/mês**

#### Configuração com High Availability
- db-f1-micro HA: ~$16/mês
- Storage SSD (20GB): ~$4/mês
- Backup (20GB): ~$2/mês
- **Total estimado: ~$22/mês**

#### Comparação com AWS RDS (sa-east-1)
| Recurso | AWS RDS | GCP Cloud SQL |
|---------|---------|---------------|
| Instância (1vCPU, 1GB) | ~$15/mês | ~$8/mês |
| Storage (20GB SSD) | ~$3/mês | ~$4/mês |
| Backup (20GB) | ~$2/mês | ~$2/mês |
| **Total** | **~$20/mês** | **~$14/mês** |

**Cloud SQL é ~30% mais barato que RDS na América do Sul!**

### Reduzir Custos

```bash
# Parar instância quando não estiver em uso (não cobra compute)
gcloud sql instances patch ${INSTANCE_NAME} \
  --activation-policy=NEVER

# Religar quando necessário
gcloud sql instances patch ${INSTANCE_NAME} \
  --activation-policy=ALWAYS

# Usar tier menor (não recomendado para produção)
gcloud sql instances patch ${INSTANCE_NAME} \
  --tier=db-g1-small
```

## 🛠️ Troubleshooting

### Erro de Conexão

```bash
# Verificar status da instância
gcloud sql instances describe ${INSTANCE_NAME} \
  --format="value(state,ipAddresses[0].ipAddress)"

# Verificar se a instância está rodando
gcloud sql instances list

# Testar conexão via Cloud SQL Proxy
./cloud-sql-proxy vschiavo-home:southamerica-east1:brewer-db --port 3307
mysql -h 127.0.0.1 -P 3307 -u admin -p
```

### Verificar Logs

```bash
# Ver logs de erro do MySQL
gcloud sql operations list --instance=${INSTANCE_NAME} --limit=10

# Ver logs via Cloud Logging
gcloud logging read "resource.type=cloudsql_database AND resource.labels.database_id=vschiavo-home:brewer-db" \
  --limit 50 \
  --format json
```

### Erros Comuns

1. **Connection refused**:
   - Instância não está rodando (state != RUNNABLE)
   - Cloud SQL Proxy não está configurado corretamente
   - Firewall bloqueando porta 3306

2. **Access denied**:
   - Verificar username/password
   - Verificar se usuário tem permissões no banco

3. **Unknown database**:
   - Criar banco via `gcloud sql databases create`

4. **SSL connection error**:
   - Ajustar parâmetros de conexão (useSSL=false para desenvolvimento)
   - Para produção, baixar certificado SSL do Cloud SQL

5. **Too many connections**:
   - Aumentar max_connections via database flags
   ```bash
   gcloud sql instances patch ${INSTANCE_NAME} \
     --database-flags=max_connections=100
   ```

### Backup e Restore

```bash
# Listar backups disponíveis
gcloud sql backups list --instance=${INSTANCE_NAME}

# Criar backup sob demanda
gcloud sql backups create --instance=${INSTANCE_NAME}

# Restaurar de um backup
gcloud sql backups restore BACKUP_ID \
  --backup-instance=${INSTANCE_NAME} \
  --backup-id=BACKUP_ID
```

### Migração de RDS para Cloud SQL

```bash
# 1. Exportar do RDS via mysqldump
mysqldump -h rds-endpoint.amazonaws.com -u admin -p brewer > brewer_backup.sql

# 2. Importar para Cloud Storage
gsutil cp brewer_backup.sql gs://brewer-backups/

# 3. Importar para Cloud SQL
gcloud sql import sql ${INSTANCE_NAME} \
  gs://brewer-backups/brewer_backup.sql \
  --database=brewer
```

## 📊 Monitoramento

### Métricas Importantes

```bash
# CPU utilization
gcloud monitoring timeseries list \
  --filter="resource.type=cloudsql_database AND metric.type=cloudsql.googleapis.com/database/cpu/utilization"

# Conexões ativas
gcloud monitoring timeseries list \
  --filter="resource.type=cloudsql_database AND metric.type=cloudsql.googleapis.com/database/mysql/connections"

# Storage usado
gcloud sql instances describe ${INSTANCE_NAME} \
  --format="value(currentDiskSize)"
```

### Alertas via Cloud Monitoring

```bash
# Criar alerta para CPU > 80%
gcloud alpha monitoring policies create \
  --notification-channels=CHANNEL_ID \
  --display-name="Cloud SQL CPU High" \
  --condition-display-name="CPU > 80%" \
  --condition-threshold-value=0.8 \
  --condition-threshold-duration=300s \
  --condition-filter='resource.type="cloudsql_database" AND metric.type="cloudsql.googleapis.com/database/cpu/utilization"'
```

## 🔄 High Availability (HA)

Para produção, configure High Availability:

```bash
# Habilitar HA (cria réplica standby em outra zona)
gcloud sql instances patch ${INSTANCE_NAME} \
  --availability-type=REGIONAL

# Verificar configuração HA
gcloud sql instances describe ${INSTANCE_NAME} \
  --format="value(settings.availabilityType)"
```

**NOTA**: HA dobra o custo da instância (~$16/mês ao invés de ~$8/mês para db-f1-micro).

## 📚 Referências

- [Cloud SQL MySQL Documentation](https://cloud.google.com/sql/docs/mysql)
- [Cloud SQL Security Best Practices](https://cloud.google.com/sql/docs/mysql/best-practices)
- [Cloud SQL Proxy Documentation](https://cloud.google.com/sql/docs/mysql/sql-proxy)
- [MySQL 8.0 Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/)
- [Migrating from AWS RDS to Cloud SQL](https://cloud.google.com/architecture/migrating-mysql-to-cloudsql-using-database-migration-service)

---

**Documentação criada em**: 2025-12-27
**Versão**: 1.0
**Autor**: Claude (Anthropic)
**Projeto**: Brewer - Sistema de Gerenciamento de Cervejaria