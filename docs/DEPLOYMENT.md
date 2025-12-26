# Deployment Guide - Brewer Application on Amazon EKS

Este guia fornece instruções completas para deploy da aplicação Brewer no Amazon EKS (Elastic Kubernetes Service).

## 📋 Pré-requisitos

### Ferramentas Necessárias
- AWS CLI (v2+): `aws --version`
- kubectl: `kubectl version --client`
- Docker: `docker --version`
- eksctl (recomendado): `eksctl version`
- gh CLI (para PRs): `gh --version`

### Permissões AWS
- Acesso ao ECR (Elastic Container Registry)
- Acesso ao EKS
- Acesso ao S3 (para fotos)
- Acesso ao RDS MySQL (se usar RDS)
- **Nota**: LoadBalancer não é mais necessário - usamos ClusterIP + Ingress

## 🏗️ Infraestrutura AWS

### 1. Criar ECR Repository

```bash
aws ecr create-repository \
    --repository-name brewer \
    --region sa-east-1 \
    --image-scanning-configuration scanOnPush=true
```

### 2. Criar EKS Cluster (se ainda não existe)

```bash
eksctl create cluster \
    --name brewer-cluster \
    --region sa-east-1 \
    --nodegroup-name brewer-nodes \
    --node-type t3.medium \
    --nodes 2 \
    --nodes-min 2 \
    --nodes-max 4 \
    --managed
```

### 3. Configurar kubectl

```bash
aws eks update-kubeconfig \
    --name brewer-cluster \
    --region sa-east-1
```

### 4. Instalar Nginx Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/cloud/deploy.yaml
```

Aguarde o LoadBalancer ser provisionado:

```bash
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

### 5. Instalar Metrics Server (para HPA)

O Metrics Server é instalado automaticamente pelo workflow CI/CD, mas você pode instalá-lo manualmente:

```bash
# Usando o manifest customizado (recomendado)
kubectl apply -f k8s/cluster-infra/metrics-server.yaml

# OU usando o manifest oficial do Kubernetes
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Verificar instalação:

```bash
# Verificar se o metrics-server está rodando
kubectl get deployment metrics-server -n kube-system

# Aguardar ficar disponível
kubectl wait --for=condition=available --timeout=2m deployment/metrics-server -n kube-system

# Testar coleta de métricas
kubectl top nodes
kubectl top pods -n brewer
```

## 🐳 Build e Push da Imagem

### Opção 1: Usando o script fornecido

```bash
# Configurar variáveis de ambiente (opcional)
export AWS_REGION=sa-east-1
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export ECR_REPOSITORY=brewer
export IMAGE_TAG=v1.0.0

# Executar script
./scripts/build-and-push.sh
```

### Opção 2: Manual

```bash
# Login no ECR
aws ecr get-login-password --region sa-east-1 | \
    docker login --username AWS --password-stdin \
    $(aws sts get-caller-identity --query Account --output text).dkr.ecr.sa-east-1.amazonaws.com

# Build
docker build -t brewer:latest .

# Tag
docker tag brewer:latest \
    $(aws sts get-caller-identity --query Account --output text).dkr.ecr.sa-east-1.amazonaws.com/brewer:latest

# Push
docker push $(aws sts get-caller-identity --query Account --output text).dkr.ecr.sa-east-1.amazonaws.com/brewer:latest
```

## 🔑 Configurar Secrets

Crie os secrets antes do deploy:

```bash
kubectl create secret generic brewer-secrets \
  --from-literal=DATABASE_PASSWORD='your-secure-password' \
  --from-literal=AWS_ACCESS_KEY_ID='your-access-key' \
  --from-literal=AWS_SECRET_ACCESS_KEY='your-secret-key' \
  --from-literal=MAIL_USERNAME='your-email@gmail.com' \
  --from-literal=MAIL_PASSWORD='your-app-password' \
  --from-literal=MAIL_FROM='noreply@brewer.com' \
  --namespace=brewer \
  --dry-run=client -o yaml | kubectl apply -f -
```

## 🚀 Deploy da Aplicação

### Opção 1: Usando o script fornecido

```bash
./scripts/deploy-to-eks.sh
```

### Opção 2: Manual

```bash
# 1. Instalar Cluster Infrastructure (metrics-server)
kubectl apply -f k8s/cluster-infra/metrics-server.yaml
kubectl wait --for=condition=available --timeout=2m deployment/metrics-server -n kube-system

# 2. Aplicar manifests da aplicação na ordem
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/resourcequota.yaml
kubectl apply -f k8s/base/configmap.yaml

# 3. Aplicar deployment com image tag correto usando sed
export ECR_REGISTRY="YOUR_ACCOUNT_ID.dkr.ecr.sa-east-1.amazonaws.com"
export ECR_REPOSITORY="brewer"
export IMAGE_TAG="latest"  # ou git commit SHA

cat k8s/base/deployment.yaml | \
  sed "s|image: brewer:latest|image: $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG|g" | \
  kubectl apply -f -

# 4. Aplicar service, HPA e PodDisruptionBudget
kubectl apply -f k8s/base/service.yaml
kubectl apply -f k8s/base/hpa.yaml
kubectl apply -f k8s/base/pdb.yaml

# 5. Aplicar Ingress (recomendado para produção)
kubectl apply -f k8s/base/ingress-nginx.yaml
```

### Estratégia de Image Tag

**IMPORTANTE**: A aplicação usa uma estratégia de image tag para evitar drift entre o Git e o cluster:

- O arquivo `k8s/base/deployment.yaml` contém `image: brewer:latest` como placeholder
- O CI/CD usa `sed` para substituir o placeholder pelo tag real antes de aplicar
- Isso garante que o manifest aplicado no cluster tenha o tag correto (commit SHA)
- **Nunca** use `kubectl set image` depois de aplicar o deployment - isso causa drift

**Por que não usar `kubectl set image`?**
- Se você aplicar o manifest depois, ele reverte para `:latest`
- Causa inconsistência entre Git e cluster
- A abordagem com `sed` mantém tudo sincronizado

```bash
# ❌ NÃO FAÇA ISSO (causa drift):
kubectl apply -f k8s/base/deployment.yaml
kubectl set image deployment/brewer-app brewer=YOUR_ECR:sha-abc123 -n brewer

# ✅ FAÇA ISSO (previne drift):
cat k8s/base/deployment.yaml | \
  sed "s|image: brewer:latest|image: $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG|g" | \
  kubectl apply -f -
```

## 📊 Verificar Deploy

### Status dos Pods

```bash
# Watch pods starting
kubectl get pods -n brewer -w

# View logs
kubectl logs -f deployment/brewer-app -n brewer

# Check all resources
kubectl get all -n brewer
```

### Status do Deployment

```bash
kubectl rollout status deployment/brewer-app -n brewer
```

### Obter URL do Ingress

**Nota**: Agora usamos ClusterIP + Ingress (não mais LoadBalancer direto no Service):

```bash
# Obter External IP do Nginx Ingress Controller
INGRESS_IP=$(kubectl get svc ingress-nginx-controller -n ingress-nginx -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "Ingress URL: http://${INGRESS_IP}"
echo "Application Domain: http://brewer.virosistemas.com"

# Verificar status do Ingress
kubectl get ingress -n brewer
kubectl describe ingress brewer-ingress-nginx -n brewer
```

### Verificar HPA (Horizontal Pod Autoscaler)

```bash
# Status atual do HPA
kubectl get hpa -n brewer

# Watch HPA em tempo real
kubectl get hpa -n brewer -w

# Detalhes completos
kubectl describe hpa brewer-app-hpa -n brewer

# Verificar se métricas estão disponíveis
kubectl top pods -n brewer
kubectl top nodes
```

**Comportamento do HPA:**
- **minReplicas**: 2 (sempre mantém no mínimo 2 pods)
- **maxReplicas**: 10 (escala até 10 pods se necessário)
- **CPU target**: 70% utilization
- **Memory target**: 80% utilization
- **Scale Up**: Rápido (60s stabilization, +100% ou +2 pods/min)
- **Scale Down**: Conservador (300s stabilization, -50% ou -1 pod/min)

### Verificar ResourceQuota

```bash
# Ver limites do namespace
kubectl describe resourcequota brewer-quota -n brewer

# Ver uso atual vs limites
kubectl get resourcequota -n brewer
```

**Limites configurados:**
- CPU requests: 4 cores (permite até 16 pods @ 250m cada)
- CPU limits: 8 cores (permite até 8 pods @ 1000m cada)
- Memory requests: 8Gi (permite até 16 pods @ 512Mi cada)
- Memory limits: 16Gi (permite até 16 pods @ 1Gi cada)
- Max pods: 20
- Max PVCs: 5
- Storage: 100Gi

**Nota**: HPA max=10 está bem dentro dos limites de ResourceQuota.

### Verificar PodDisruptionBudget

```bash
# Verificar PDB
kubectl get pdb -n brewer
kubectl describe pdb brewer-app-pdb -n brewer
```

O PDB garante que **minAvailable: 1** pod esteja sempre rodando durante:
- Node drains (manutenção de nós)
- Cluster upgrades
- Voluntary disruptions

### Verificar Actuator Endpoints

```bash
# Health check público (porta 8080)
kubectl port-forward -n brewer deployment/brewer-app 8080:8080
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info

# Metrics interno (porta 9090 - não exposto publicamente)
kubectl port-forward -n brewer deployment/brewer-app 9090:9090
curl http://localhost:9090/actuator/metrics
curl http://localhost:9090/actuator/prometheus
```

**Nota de Segurança**: Apenas `health` e `info` estão expostos na porta principal. Métricas sensíveis (prometheus, metrics) estão isoladas na porta 9090.

## 🔧 Configurações Importantes

### ConfigMap (k8s/base/configmap.yaml)

Ajuste conforme seu ambiente:
- `DATABASE_URL`: URL do MySQL (RDS ou outro)
- `AWS_REGION`: Região AWS
- `AWS_S3_BUCKET`: Nome do bucket S3
- `MAIL_HOST`: Servidor SMTP

### Deployment (k8s/base/deployment.yaml)

Ajuste recursos conforme necessidade:
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### HPA (k8s/base/hpa.yaml)

Ajuste escalabilidade:
```yaml
minReplicas: 2
maxReplicas: 10
```

## 🗄️ Database (MySQL)

### Opção 1: Amazon RDS MySQL

Recomendado para produção. Configure Multi-AZ para alta disponibilidade.

```bash
# Exemplo de criação RDS
aws rds create-db-instance \
    --db-instance-identifier brewer-db \
    --db-instance-class db.t3.micro \
    --engine mysql \
    --engine-version 8.0.35 \
    --master-username admin \
    --master-user-password YourSecurePassword \
    --allocated-storage 20 \
    --multi-az \
    --db-name brewer \
    --vpc-security-group-ids sg-xxxxx \
    --region sa-east-1
```

Atualize o ConfigMap com o endpoint do RDS.

### Opção 2: MySQL no Kubernetes

Para desenvolvimento/testes apenas. Não recomendado para produção.

## 🔄 Atualizações e Rollback

### Deploy de Nova Versão

```bash
# Build nova versão
export IMAGE_TAG=v1.1.0
./scripts/build-and-push.sh

# Atualizar deployment
kubectl set image deployment/brewer-app \
    brewer=YOUR_ACCOUNT_ID.dkr.ecr.sa-east-1.amazonaws.com/brewer:v1.1.0 \
    -n brewer

# Acompanhar rollout
kubectl rollout status deployment/brewer-app -n brewer
```

### Rollback

```bash
# Ver histórico
kubectl rollout history deployment/brewer-app -n brewer

# Rollback para versão anterior
kubectl rollout undo deployment/brewer-app -n brewer

# Rollback para revisão específica
kubectl rollout undo deployment/brewer-app -n brewer --to-revision=2
```

## 📈 Monitoramento

### Logs

```bash
# Logs de todos os pods
kubectl logs -f -l app=brewer -n brewer

# Logs de um pod específico
kubectl logs -f pod/brewer-app-xxxxx -n brewer

# Logs anteriores (após restart)
kubectl logs --previous pod/brewer-app-xxxxx -n brewer
```

### Métricas

```bash
# CPU e Memória dos pods
kubectl top pods -n brewer

# Status do HPA
kubectl get hpa -n brewer -w
```

### Exec no Container

```bash
kubectl exec -it deployment/brewer-app -n brewer -- /bin/sh
```

## 🛡️ Segurança

### Melhorias de Segurança Implementadas

A aplicação implementa várias camadas de segurança:

#### 1. Actuator Endpoints Protegidos

```yaml
# Somente health e info expostos publicamente (porta 8080)
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info"

# Métricas sensíveis isoladas em porta separada (porta 9090)
MANAGEMENT_SERVER_PORT: "9090"
```

**Por que isso é importante:**
- Métricas Prometheus e outros endpoints sensíveis não estão expostos publicamente
- Reduz superfície de ataque
- Métricas ainda acessíveis internamente para monitoring

#### 2. Rate Limiting no Ingress

```yaml
# Proteção contra DDoS e abuso
nginx.ingress.kubernetes.io/limit-rps: "10"
nginx.ingress.kubernetes.io/limit-rpm: "100"
nginx.ingress.kubernetes.io/limit-connections: "10"
```

**Proteções:**
- Máximo 10 requests por segundo por IP
- Máximo 100 requests por minuto por IP
- Máximo 10 conexões simultâneas por IP

#### 3. Session Cookie Seguro

```yaml
# Cookie de sessão com duração limitada
nginx.ingress.kubernetes.io/session-cookie-max-age: "28800"  # 8 horas
```

**Antes:** 48 horas (muito tempo para dados sensíveis)
**Agora:** 8 horas (balanceando UX e segurança)

#### 4. ResourceQuota (Proteção contra Resource Exhaustion)

```yaml
# Limita recursos do namespace
requests.cpu: "4"
limits.cpu: "8"
requests.memory: 8Gi
limits.memory: 16Gi
pods: "20"
```

**Previne:**
- Pods descontrolados consumindo todo o cluster
- Ataques de resource exhaustion
- Bills inesperadas de cloud

#### 5. PodDisruptionBudget (Alta Disponibilidade)

```yaml
# Garante disponibilidade durante maintenance
minAvailable: 1
```

**Garante:**
- Pelo menos 1 pod sempre rodando
- Proteção durante node drains e cluster upgrades
- Zero downtime durante manutenções programadas

#### 6. Flyway Repair Otimizado

```yaml
# Job não inicia aplicação completa, apenas reparo
args:
  - "--spring.main.web-application-type=none"
  - "--spring.flyway.repair-on-migrate=true"
```

**Antes:** Iniciava toda a aplicação Spring Boot
**Agora:** Executa apenas reparo de migrations
**Benefício:** Mais rápido, menos recursos, mais seguro

#### 7. Startup Probe Otimizado

```yaml
# Detecta pods lentos rapidamente
initialDelaySeconds: 0
periodSeconds: 5
failureThreshold: 60  # 5 * 60 = 300s timeout
```

**Benefício:** Detecta falhas de startup mais rápido, previne pods stuck

#### 8. Service Type ClusterIP

**Antes:** LoadBalancer (AWS NLB adicional = custo extra)
**Agora:** ClusterIP com Ingress (único LoadBalancer)
**Benefício:** Menos custos, mesma funcionalidade

### Secrets Management

**Importante:** Nunca commite secrets no Git!

Para ambientes de produção, considere usar:
- **AWS Secrets Manager + External Secrets Operator** (recomendado para AWS)
- **HashiCorp Vault**
- **Sealed Secrets**

### Network Policies

Implementar Network Policies para isolar o namespace:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: brewer-netpol
  namespace: brewer
spec:
  podSelector:
    matchLabels:
      app: brewer
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 3306  # MySQL
    - protocol: TCP
      port: 443   # HTTPS
```

### Checklist de Segurança

- [x] Actuator endpoints sensíveis isolados (porta 9090)
- [x] Rate limiting configurado no Ingress
- [x] Session cookie com duração apropriada (8h)
- [x] ResourceQuota configurado
- [x] PodDisruptionBudget configurado
- [x] Startup probe otimizado
- [x] Service tipo ClusterIP (não LoadBalancer)
- [x] Flyway repair otimizado
- [x] Secrets gerenciados via Kubernetes Secrets
- [ ] TLS/HTTPS configurado (próximo passo)
- [ ] Network Policies implementadas (opcional)
- [ ] AWS Secrets Manager integrado (recomendado para produção)

## 🧪 Testar Auto-Scaling (HPA)

### Teste de Carga Simples

```bash
# 1. Verificar estado inicial
kubectl get hpa -n brewer
kubectl get pods -n brewer

# 2. Gerar carga (usando Apache Bench)
kubectl run -i --tty load-generator --rm --image=busybox:1.28 --restart=Never -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://brewer-app.brewer.svc.cluster.local/actuator/health; done"

# 3. Em outro terminal, watch HPA e pods
kubectl get hpa -n brewer -w
kubectl get pods -n brewer -w

# 4. Ver métricas em tempo real
kubectl top pods -n brewer
```

### Comportamento Esperado

**Scale Up** (quando CPU > 70% ou Memory > 80%):
- HPA detecta alta utilização
- Aguarda 60s de estabilização
- Aumenta pods em até 100% ou +2 pods/min
- Pods adicionais são criados
- Carga é distribuída

**Scale Down** (quando uso normaliza):
- HPA detecta baixa utilização
- Aguarda 300s (5 minutos) de estabilização
- Reduz pods em até 50% ou -1 pod/min
- Nunca desce abaixo de minReplicas: 2
- PDB garante minAvailable: 1 durante scale down

### Exemplo de Teste com Curl

```bash
# Gerar requests em loop
for i in {1..10000}; do
  curl -s http://brewer.virosistemas.com/actuator/health > /dev/null
  echo "Request $i"
  sleep 0.1
done
```

## 🚨 Troubleshooting

### Pod não inicia

```bash
# Descrever pod para ver eventos
kubectl describe pod brewer-app-xxxxx -n brewer

# Ver logs do pod
kubectl logs brewer-app-xxxxx -n brewer

# Ver logs anteriores (se pod reiniciou)
kubectl logs --previous brewer-app-xxxxx -n brewer

# Verificar eventos do namespace
kubectl get events -n brewer --sort-by='.lastTimestamp'
```

**Problemas comuns:**
- `ImagePullBackOff`: Verificar se a imagem existe no ECR e se IRSA/credentials estão corretos
- `CrashLoopBackOff`: Verificar logs do pod e configuração do ConfigMap/Secrets
- `Pending`: Verificar ResourceQuota, pode estar sem recursos disponíveis

### HPA não está escalando

```bash
# 1. Verificar se metrics-server está rodando
kubectl get deployment metrics-server -n kube-system
kubectl logs -n kube-system deployment/metrics-server

# 2. Verificar se métricas estão disponíveis
kubectl top nodes
kubectl top pods -n brewer

# 3. Verificar HPA status
kubectl describe hpa brewer-app-hpa -n brewer

# 4. Verificar eventos do HPA
kubectl get events -n brewer | grep HorizontalPodAutoscaler
```

**Problemas comuns:**
- Métricas não disponíveis: metrics-server não está instalado ou com problemas
- `unable to get metrics`: Aguardar alguns minutos após deploy
- HPA mostra `<unknown>`: Pods ainda não têm métricas suficientes
- Não escala: Verificar se CPU/Memory estão realmente acima dos targets

### Metrics Server não funciona

```bash
# Verificar logs
kubectl logs -n kube-system deployment/metrics-server --tail=50

# Verificar API
kubectl get apiservices | grep metrics

# Se API não está disponível, reinstalar
kubectl delete -f k8s/cluster-infra/metrics-server.yaml
kubectl apply -f k8s/cluster-infra/metrics-server.yaml
kubectl wait --for=condition=available --timeout=2m deployment/metrics-server -n kube-system
```

### ResourceQuota está bloqueando pods

```bash
# Ver uso atual vs limites
kubectl describe resourcequota brewer-quota -n brewer

# Ver recursos de todos os pods
kubectl describe pods -n brewer | grep -A 5 "Requests:"
```

**Solução**: Se atingiu os limites, você tem duas opções:
1. Reduzir recursos dos pods em `deployment.yaml`
2. Aumentar limites do ResourceQuota em `resourcequota.yaml`

### Problemas de conectividade com Ingress

```bash
# Verificar Ingress Controller
kubectl get pods -n ingress-nginx
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller

# Verificar Ingress resource
kubectl describe ingress brewer-ingress-nginx -n brewer

# Verificar Service
kubectl get svc brewer-app -n brewer
kubectl get endpoints brewer-app -n brewer

# Testar conectividade interna
kubectl run test --rm -it --image=busybox --restart=Never -- wget -O- http://brewer-app.brewer.svc.cluster.local/actuator/health
```

**Rate Limiting**: Ingress tem rate limiting configurado:
- 10 RPS (requests per second)
- 100 RPM (requests per minute)
- 10 conexões simultâneas

Se estiver sendo bloqueado, ajuste em `k8s/base/ingress-nginx.yaml`.

### Problemas com secrets

```bash
# Listar secrets
kubectl get secrets -n brewer

# Descrever secret (não mostra valores)
kubectl describe secret brewer-secrets -n brewer

# Ver secret (base64 encoded)
kubectl get secret brewer-secrets -n brewer -o yaml

# Decodificar um valor específico
kubectl get secret brewer-secrets -n brewer -o jsonpath='{.data.DATABASE_PASSWORD}' | base64 -d
```

### Flyway Migration falha

```bash
# Ver logs do job de reparo
kubectl logs -n brewer job/flyway-repair-job

# Ver todos os jobs
kubectl get jobs -n brewer

# Deletar job antigo e recriar
kubectl delete job flyway-repair-job -n brewer
kubectl apply -f k8s/base/flyway-repair-job.yaml
```

### PodDisruptionBudget está bloqueando drain

```bash
# Ver status do PDB
kubectl get pdb -n brewer
kubectl describe pdb brewer-app-pdb -n brewer

# Se precisar drenar nó mesmo assim (cuidado!)
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data --disable-eviction
```

## 🔗 Recursos Úteis

- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)
- [Metrics Server](https://github.com/kubernetes-sigs/metrics-server)

## 📝 Checklist de Deploy

### Infraestrutura
- [ ] ECR repository criado
- [ ] EKS cluster configurado e rodando
- [ ] kubectl configurado (conexão com cluster)
- [ ] Nginx Ingress Controller instalado
- [ ] Metrics Server instalado (automático via CI/CD)

### Aplicação
- [ ] Imagem Docker buildada e pushed para ECR
- [ ] Namespace `brewer` criado
- [ ] ResourceQuota aplicado
- [ ] Secrets configurados (DATABASE_PASSWORD, AWS credentials, MAIL)
- [ ] ConfigMap ajustado para seu ambiente
- [ ] Database (RDS MySQL) configurado e acessível
- [ ] S3 bucket configurado para fotos

### Deploy
- [ ] Deployment aplicado (com sed para image tag)
- [ ] Service (ClusterIP) criado
- [ ] HPA configurado e funcionando
- [ ] PodDisruptionBudget aplicado
- [ ] Ingress configurado (brewer.virosistemas.com)
- [ ] Pods rodando e healthy (min 2 replicas)

### Verificação
- [ ] `kubectl get pods -n brewer` mostra pods Running
- [ ] `kubectl get hpa -n brewer` mostra métricas
- [ ] `kubectl top pods -n brewer` mostra CPU/Memory
- [ ] Ingress acessível via domínio
- [ ] Health check responde: `curl http://brewer.virosistemas.com/actuator/health`
- [ ] Logs sem erros: `kubectl logs -f deployment/brewer-app -n brewer`
- [ ] ResourceQuota dentro dos limites
- [ ] PDB protegendo pods

### Segurança
- [ ] Rate limiting configurado no Ingress
- [ ] Actuator metrics isolados (porta 9090)
- [ ] Session cookie com 8h de duração
- [ ] Secrets não commitados no Git

## 🎯 Próximos Passos

### Concluído
- [x] CI/CD configurado (GitHub Actions)
- [x] Auto-scaling implementado (HPA)
- [x] Rate limiting e proteção DDoS
- [x] Segurança dos endpoints Actuator
- [x] ResourceQuota e PodDisruptionBudget
- [x] Metrics Server para monitoramento básico

### Recomendado
1. **Configurar SSL/TLS**
   - Obter certificado SSL via Cloudflare ou AWS Certificate Manager
   - Habilitar TLS no Ingress
   - Forçar HTTPS redirect

2. **Implementar monitoramento avançado**
   - Prometheus para coleta de métricas
   - Grafana para dashboards
   - Loki para agregação de logs

3. **Configurar alertas**
   - AlertManager para alertas do Prometheus
   - CloudWatch Alarms para métricas AWS
   - PagerDuty/Slack integration

4. **Backup automático**
   - RDS automated backups (já incluso se usar RDS)
   - S3 bucket versioning para fotos
   - Velero para backup do cluster

5. **Disaster Recovery**
   - Multi-AZ RDS (alta disponibilidade)
   - Cross-region replication do S3
   - EKS cluster backup strategy

6. **Observabilidade**
   - Distributed tracing (Jaeger ou AWS X-Ray)
   - APM (Application Performance Monitoring)
   - Error tracking (Sentry)

7. **Melhorias de Segurança**
   - Migrar secrets para AWS Secrets Manager
   - Implementar Network Policies
   - Pod Security Standards
   - Vulnerability scanning (Trivy, Snyk)

8. **Performance**
   - Configurar CDN (CloudFront) para assets estáticos
   - Implementar Redis para cache
   - Database query optimization
