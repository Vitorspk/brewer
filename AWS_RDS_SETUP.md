# AWS RDS MySQL - Configuração do Banco de Dados Brewer

## 📊 Informações da Instância RDS

A aplicação Brewer agora utiliza um banco de dados MySQL hospedado na AWS RDS.

### Detalhes da Instância

- **Instance ID**: `brewer-db`
- **Endpoint**: `brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com`
- **Porta**: `3306`
- **Engine**: MySQL 8.0.40
- **Instance Class**: db.t3.micro (1 vCPU, 1GB RAM)
- **Storage**: 20GB gp3 (3000 IOPS, 125 MB/s throughput)
- **Região**: sa-east-1 (São Paulo)
- **Availability Zone**: sa-east-1a
- **Publicly Accessible**: Sim
- **Multi-AZ**: Não (para economia de custos)
- **Backup Retention**: 7 dias

## 🔐 Credenciais

As credenciais estão no arquivo `.env.rds` (não versionado):

```bash
Host: brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com
Port: 3306
Username: admin
Password: BrewerAdmin2024
Database: brewer
Test Database: brewer_test (precisa ser criado)
```

## 🚀 Uso

### 1. Criar Banco de Testes

O banco principal `brewer` já foi criado automaticamente. Você precisa criar o banco de testes:

```sql
CREATE DATABASE IF NOT EXISTS brewer_test;
```

#### Via MySQL Client (se instalado)

```bash
mysql -h brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com -u admin -pBrewerAdmin2024 -e "CREATE DATABASE IF NOT EXISTS brewer_test;"
```

#### Via Docker

```bash
docker run -it --rm mysql:8.0 mysql \
  -h brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com \
  -u admin \
  -pBrewerAdmin2024 \
  -e "CREATE DATABASE IF NOT EXISTS brewer_test;"
```

### 2. Configurar Application Properties

#### application.properties (Produção)

```properties
spring.datasource.url=jdbc:mysql://brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com:3306/brewer?allowPublicKeyRetrieval=true&useSSL=true&requireSSL=false
spring.datasource.username=admin
spring.datasource.password=BrewerAdmin2024
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
```

#### application-test.properties (Testes)

```properties
spring.datasource.url=jdbc:mysql://brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com:3306/brewer_test?allowPublicKeyRetrieval=true&useSSL=true&requireSSL=false
spring.datasource.username=admin
spring.datasource.password=BrewerAdmin2024
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### 3. Usar com Docker Compose

Atualize o `docker-compose.yml` para usar o RDS ao invés do MySQL local:

```yaml
services:
  brewer-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com:3306/brewer?allowPublicKeyRetrieval=true&useSSL=true&requireSSL=false
      - SPRING_DATASOURCE_USERNAME=admin
      - SPRING_DATASOURCE_PASSWORD=BrewerAdmin2024
      - SPRING_PROFILES_ACTIVE=prod
    # Remover a dependência do MySQL local
    # depends_on:
    #   - mysql
```

### 4. Variáveis de Ambiente para CI/CD

Para uso no GitHub Actions CI:

```yaml
env:
  SPRING_DATASOURCE_URL: jdbc:mysql://brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com:3306/brewer_test?allowPublicKeyRetrieval=true&useSSL=true&requireSSL=false
  SPRING_DATASOURCE_USERNAME: admin
  SPRING_DATASOURCE_PASSWORD: ${{ secrets.RDS_PASSWORD }}
  TEST_DB_URL: jdbc:mysql://brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com:3306/brewer_test?allowPublicKeyRetrieval=true&useSSL=true&requireSSL=false
  TEST_DB_USER: admin
  TEST_DB_PASSWORD: ${{ secrets.RDS_PASSWORD }}
```

**⚠️ Importante**: Adicione `RDS_PASSWORD` como secret no GitHub:
- Vá em Settings > Secrets and variables > Actions
- Adicione: `RDS_PASSWORD` = `BrewerAdmin2024`

## 🛠️ Comandos AWS CLI Úteis

### Verificar Status da Instância

```bash
aws rds describe-db-instances --db-instance-identifier brewer-db --query 'DBInstances[0].{Status:DBInstanceStatus,Endpoint:Endpoint.Address,Port:Endpoint.Port}'
```

### Ver Informações Completas

```bash
aws rds describe-db-instances --db-instance-identifier brewer-db
```

### Criar Snapshot (Backup Manual)

```bash
aws rds create-db-snapshot \
  --db-instance-identifier brewer-db \
  --db-snapshot-identifier brewer-db-snapshot-$(date +%Y%m%d-%H%M%S)
```

### Modificar Instância (exemplo: mudar password)

```bash
aws rds modify-db-instance \
  --db-instance-identifier brewer-db \
  --master-user-password NovoPassword123 \
  --apply-immediately
```

### Parar Instância (economia de custos)

```bash
aws rds stop-db-instance --db-instance-identifier brewer-db
```

### Iniciar Instância

```bash
aws rds start-db-instance --db-instance-identifier brewer-db
```

### Deletar Instância (CUIDADO!)

```bash
# Sem snapshot final
aws rds delete-db-instance \
  --db-instance-identifier brewer-db \
  --skip-final-snapshot

# Com snapshot final (recomendado)
aws rds delete-db-instance \
  --db-instance-identifier brewer-db \
  --final-db-snapshot-identifier brewer-db-final-snapshot
```

## 🔒 Segurança

### Security Group

- **ID**: `sg-0cf3cf89d59c0977d`
- **Nome**: `brewer-rds-sg`
- **Regras**:
  - Inbound: TCP 3306 de 0.0.0.0/0 (qualquer IP)
  - ⚠️ **Para produção**: Restrinja para IPs específicos ou VPC

### Subnet Group

- **Nome**: `brewer-db-subnet-group`
- **VPC**: `vpc-51181f36` (default)
- **Subnets**:
  - subnet-0fd06069 (sa-east-1a)
  - subnet-8afe48c3 (sa-east-1b)
  - subnet-9167d8ca (sa-east-1c)

## 💰 Custos Estimados

Com a configuração atual (db.t3.micro, 20GB gp3, sa-east-1):

- **Instância**: ~$15/mês (750 horas free tier no primeiro ano)
- **Storage**: ~$2/mês (20GB gp3)
- **Backup**: Grátis (até 20GB = tamanho da instância)
- **Transfer**: Variável (dependendo do uso)

**Total estimado**: ~$17/mês (após free tier)

## 📝 Notas Importantes

1. ⚠️ **Segurança**: A instância está acessível publicamente (0.0.0.0/0). Para produção, restrinja o acesso.
2. 🔄 **Backups**: Configurado para retenção de 7 dias.
3. 🚀 **Auto Minor Version Upgrade**: Habilitado - a AWS aplicará patches de segurança automaticamente.
4. 💾 **Storage**: gp3 com 3000 IOPS base e 125 MB/s throughput.
5. 🌍 **Single-AZ**: Instância em uma única zona de disponibilidade (sem redundância automática).
6. 🔐 **Credenciais**: Armazenadas no arquivo `.env.rds` (não commitar!).
7. 📊 **Monitoramento**: CloudWatch metrics habilitado por padrão.

## 🆘 Troubleshooting

### Erro: "Communications link failure"

1. Verifique se o security group permite seu IP:
   ```bash
   aws ec2 describe-security-groups --group-ids sg-0cf3cf89d59c0977d
   ```

2. Verifique se a instância está rodando:
   ```bash
   aws rds describe-db-instances --db-instance-identifier brewer-db --query 'DBInstances[0].DBInstanceStatus'
   ```

### Erro: "Access denied for user"

- Verifique usuário/senha no `.env.rds`
- Se necessário, reset da senha via AWS CLI (comando acima)

### Erro: "Unknown database 'brewer_test'"

- Crie o banco de testes conforme instruções na seção "Criar Banco de Testes"

## 📚 Referências

- [AWS RDS MySQL Documentation](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_MySQL.html)
- [RDS Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)
- [RDS Security](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.html)