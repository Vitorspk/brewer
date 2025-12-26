# Kubernetes Manifests

Este diretório contém os manifestos Kubernetes para deploy da aplicação Brewer.

## Estrutura

```
k8s/
├── base/           # Manifestos base do Kubernetes
│   ├── configmap.yaml      # ConfigMap com variáveis de ambiente
│   ├── deployment.yaml     # Deployment da aplicação
│   ├── hpa.yaml           # HorizontalPodAutoscaler
│   ├── namespace.yaml     # Namespace brewer
│   ├── secrets.yaml       # Secrets (template)
│   └── service.yaml       # Service LoadBalancer
└── README.md      # Este arquivo
```

## Validações Automatizadas

Os manifestos Kubernetes passam por validações automatizadas no GitHub Actions:

### 1. **Kubeval** - Validação de Sintaxe
Valida se os manifestos seguem o schema do Kubernetes.

```bash
kubeval --strict --ignore-missing-schemas k8s/base/*.yaml
```

### 2. **Kube-score** - Análise de Qualidade
Analisa os manifestos e fornece recomendações de melhores práticas.

```bash
kube-score score k8s/base/*.yaml
```

Testes ignorados:
- `pod-networkpolicy` - NetworkPolicy opcional para este ambiente
- `deployment-has-poddisruptionbudget` - PDB opcional para clusters pequenos
- `container-security-context-user-group-id` - IDs específicos do container

### 3. **Kube-linter** - Segurança
Verifica configurações de segurança e possíveis vulnerabilidades.

```bash
kube-linter lint k8s/base/ --config .kube-linter.yaml
```

Configuração em [`.kube-linter.yaml`](../.kube-linter.yaml)

### 4. **Kubectl Dry-run** - Teste de Deploy
Valida se os manifestos podem ser aplicados no cluster (validação sintática).

```bash
kubectl apply --dry-run=client -f k8s/base/
```

## Executar Validações Localmente

### Pré-requisitos

Instale as ferramentas:

```bash
# Kubeval
wget https://github.com/instrumenta/kubeval/releases/latest/download/kubeval-linux-amd64.tar.gz
tar xf kubeval-linux-amd64.tar.gz
sudo mv kubeval /usr/local/bin

# Kube-score
wget https://github.com/zegl/kube-score/releases/download/v1.18.0/kube-score_1.18.0_linux_amd64.tar.gz
tar xf kube-score_1.18.0_linux_amd64.tar.gz
sudo mv kube-score /usr/local/bin

# Kube-linter
wget https://github.com/stackrox/kube-linter/releases/download/v0.6.8/kube-linter-linux.tar.gz
tar xf kube-linter-linux.tar.gz
sudo mv kube-linter /usr/local/bin
```

### Executar Validações

```bash
# Validar sintaxe
kubeval --strict --ignore-missing-schemas k8s/base/*.yaml

# Analisar qualidade
kube-score score k8s/base/*.yaml

# Verificar segurança
kube-linter lint k8s/base/ --config .kube-linter.yaml

# Dry-run (requer kubectl configurado)
kubectl apply --dry-run=client -f k8s/base/
```

## Deploy

### AWS EKS

O deploy é realizado automaticamente via GitHub Actions workflow `Deploy to EKS`.

**Workflow:** [`.github/workflows/deploy-to-eks.yml`](../.github/workflows/deploy-to-eks.yml)

**Manual:**
```bash
# Configure AWS CLI e kubectl
aws eks update-kubeconfig --region sa-east-1 --name eks-dev

# Apply manifests
kubectl apply -f k8s/base/

# Verificar status
kubectl get pods -n brewer
kubectl get svc -n brewer
kubectl get hpa -n brewer
```

## Configuração

### ConfigMap
Variáveis de ambiente não-sensíveis em [`configmap.yaml`](base/configmap.yaml):
- Spring profiles
- Portas da aplicação
- Configurações do Actuator
- Configurações de probes do Kubernetes

### Secrets
Credenciais sensíveis em [`secrets.yaml`](base/secrets.yaml) (template):
- Credenciais do banco de dados
- Credenciais AWS
- Credenciais de email

**Importante:** Os secrets devem ser criados manualmente no cluster ou via GitHub Secrets.

### HPA (Horizontal Pod Autoscaler)
Configurado em [`hpa.yaml`](base/hpa.yaml):
- **Min replicas:** 1
- **Max replicas:** 2
- **CPU target:** 70%
- **Memory target:** 80%

### Health Probes
Configurados em [`deployment.yaml`](base/deployment.yaml):

**Startup Probe:**
- Path: `/actuator/health/liveness`
- Initial delay: 0s
- Period: 5s
- Failure threshold: 60 (5 minutos total)

**Liveness Probe:**
- Path: `/actuator/health/liveness`
- Initial delay: 60s
- Period: 10s
- Failure threshold: 3

**Readiness Probe:**
- Path: `/actuator/health/readiness`
- Initial delay: 30s
- Period: 5s
- Failure threshold: 3

## Troubleshooting

### Pods não ficam Ready

```bash
# Ver logs do pod
kubectl logs -n brewer <pod-name>

# Ver eventos do pod
kubectl describe pod -n brewer <pod-name>

# Testar health endpoints
kubectl exec -n brewer <pod-name> -- wget -qO- http://localhost:8080/actuator/health/liveness
kubectl exec -n brewer <pod-name> -- wget -qO- http://localhost:8080/actuator/health/readiness
```

### HPA não está escalando

```bash
# Ver status do HPA
kubectl get hpa -n brewer

# Ver métricas dos pods
kubectl top pods -n brewer

# Ver métricas dos nodes
kubectl top nodes
```

### Service não está acessível

```bash
# Ver status do service
kubectl get svc -n brewer

# Ver endpoints
kubectl get endpoints -n brewer

# Verificar LoadBalancer
kubectl describe svc brewer-service -n brewer
```

## Segurança

### Security Context
Os pods executam com as seguintes restrições de segurança:

- `runAsNonRoot: true` - Não permite execução como root
- `runAsUser: 1000` - UID específico do usuário
- `allowPrivilegeEscalation: false` - Previne escalação de privilégios
- `capabilities.drop: [ALL]` - Remove todas as capabilities Linux
- `readOnlyRootFilesystem: false` - Permite escrita em /tmp e /app/fotos

### Network Policy
Considere adicionar NetworkPolicy para restringir comunicação entre pods.

### Pod Disruption Budget
Para ambientes de produção com alta disponibilidade, considere adicionar PDB:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: brewer-app-pdb
  namespace: brewer
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: brewer
      component: application
```

## Referências

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [AWS EKS Best Practices](https://aws.github.io/aws-eks-best-practices/)
- [Kubeval](https://kubeval.instrumenta.dev/)
- [Kube-score](https://github.com/zegl/kube-score)
- [Kube-linter](https://docs.kubelinter.io/)