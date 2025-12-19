#!/bin/bash
# Script to deploy application to EKS

set -e

# Configuration
NAMESPACE="${NAMESPACE:-brewer}"
KUBECTL="${KUBECTL:-kubectl}"

echo "==================================="
echo "Deploying Brewer to EKS"
echo "==================================="
echo "Namespace: ${NAMESPACE}"
echo "==================================="

# Create namespace if it doesn't exist
echo "Creating namespace..."
${KUBECTL} apply -f k8s/base/namespace.yaml

# Apply ConfigMap
echo "Applying ConfigMap..."
${KUBECTL} apply -f k8s/base/configmap.yaml

# Check if secrets exist, if not warn user
if ! ${KUBECTL} get secret brewer-secrets -n ${NAMESPACE} &> /dev/null; then
    echo "⚠️  WARNING: Secret 'brewer-secrets' not found!"
    echo "Please create secrets before deploying:"
    echo ""
    echo "kubectl create secret generic brewer-secrets \\"
    echo "  --from-literal=DATABASE_PASSWORD='your-password' \\"
    echo "  --from-literal=AWS_ACCESS_KEY_ID='your-access-key' \\"
    echo "  --from-literal=AWS_SECRET_ACCESS_KEY='your-secret-key' \\"
    echo "  --from-literal=MAIL_USERNAME='your-email' \\"
    echo "  --from-literal=MAIL_PASSWORD='your-app-password' \\"
    echo "  --from-literal=MAIL_FROM='noreply@brewer.com' \\"
    echo "  --namespace=${NAMESPACE}"
    echo ""
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Apply Deployment
echo "Applying Deployment..."
${KUBECTL} apply -f k8s/base/deployment.yaml

# Apply Service
echo "Applying Service..."
${KUBECTL} apply -f k8s/base/service.yaml

# Apply HPA
echo "Applying HPA..."
${KUBECTL} apply -f k8s/base/hpa.yaml

# Wait for deployment
echo "Waiting for deployment to be ready..."
${KUBECTL} rollout status deployment/brewer-app -n ${NAMESPACE} --timeout=300s

# Get service endpoint
echo ""
echo "==================================="
echo "✅ Deployment completed!"
echo "==================================="
${KUBECTL} get pods -n ${NAMESPACE}
echo ""
${KUBECTL} get svc -n ${NAMESPACE}
echo ""
echo "To get the LoadBalancer URL:"
echo "${KUBECTL} get svc brewer-app -n ${NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'"
