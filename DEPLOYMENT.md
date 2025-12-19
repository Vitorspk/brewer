# Deployment Guide - Brewer Application on Amazon EKS

Este guia fornece instruções completas para deploy da aplicação Brewer no Amazon EKS (Elastic Kubernetes Service).

## 📋 Pré-requisitos

### Ferramentas Necessárias
- AWS CLI (v2+): `aws --version`
- kubectl: `kubectl version --client`
- Docker: `docker --version`
- eksctl (recomendado): `eksctl version`

### Permissões AWS
- Acesso ao ECR (Elastic Container Registry)
- Acesso ao EKS
- Permissões para criar LoadBalancer
- Acesso ao S3 (para fotos)
- Acesso ao RDS MySQL (se usar RDS)

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

### 4. Instalar Metrics Server (para HPA)

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
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
# Aplicar manifests na ordem
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/base/deployment.yaml
kubectl apply -f k8s/base/service.yaml
kubectl apply -f k8s/base/hpa.yaml

# Opcional: Ingress (se usar AWS Load Balancer Controller)
kubectl apply -f k8s/base/ingress.yaml
```

### Atualizar a imagem no Deployment

Antes de aplicar o deployment, atualize a imagem no arquivo `k8s/base/deployment.yaml`:

```yaml
containers:
- name: brewer
  image: YOUR_ACCOUNT_ID.dkr.ecr.sa-east-1.amazonaws.com/brewer:latest
```

Ou use kubectl set image:

```bash
kubectl set image deployment/brewer-app \
    brewer=YOUR_ACCOUNT_ID.dkr.ecr.sa-east-1.amazonaws.com/brewer:latest \
    -n brewer
```

## 📊 Verificar Deploy

### Status dos Pods

```bash
kubectl get pods -n brewer -w
kubectl logs -f deployment/brewer-app -n brewer
```

### Status do Deployment

```bash
kubectl rollout status deployment/brewer-app -n brewer
```

### Obter URL do LoadBalancer

```bash
kubectl get svc brewer-app -n brewer
```

Ou diretamente:

```bash
LB_URL=$(kubectl get svc brewer-app -n brewer -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "Application URL: http://${LB_URL}"
```

### Verificar HPA

```bash
kubectl get hpa -n brewer
```

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

### Secrets Management

Para ambientes de produção, considere usar:
- AWS Secrets Manager + External Secrets Operator
- HashiCorp Vault
- Sealed Secrets

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

## 🚨 Troubleshooting

### Pod não inicia

```bash
kubectl describe pod brewer-app-xxxxx -n brewer
kubectl logs brewer-app-xxxxx -n brewer
```

### Problemas de conectividade

```bash
# Testar conectividade interna
kubectl run test --rm -it --image=busybox --restart=Never -- wget -O- brewer-app.brewer.svc.cluster.local
```

### Problemas com secrets

```bash
kubectl get secrets -n brewer
kubectl describe secret brewer-secrets -n brewer
```

## 🔗 Recursos Úteis

- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)
- [Metrics Server](https://github.com/kubernetes-sigs/metrics-server)

## 📝 Checklist de Deploy

- [ ] ECR repository criado
- [ ] EKS cluster configurado
- [ ] kubectl configurado
- [ ] Metrics Server instalado
- [ ] Imagem Docker buildada e pushed
- [ ] Secrets configurados
- [ ] ConfigMap ajustado para seu ambiente
- [ ] Database (RDS) configurado e acessível
- [ ] S3 bucket configurado
- [ ] Deploy realizado com sucesso
- [ ] Pods rodando e healthy
- [ ] LoadBalancer acessível
- [ ] HPA funcionando
- [ ] Logs e métricas verificados

## 🎯 Próximos Passos

1. Configurar CI/CD (GitHub Actions, GitLab CI, etc.)
2. Implementar monitoramento (Prometheus + Grafana)
3. Configurar alertas (AlertManager, CloudWatch)
4. Implementar backup automático do banco
5. Configurar SSL/TLS com certificado (AWS Certificate Manager + ALB Ingress)
6. Implementar estratégia de disaster recovery
