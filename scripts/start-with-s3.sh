#!/bin/bash
# Script para iniciar a aplicação Brewer com AWS S3 Storage
# Uso: ./scripts/start-with-s3.sh

set -e

echo "=============================================="
echo "🚀 Iniciando Brewer com AWS S3 Storage"
echo "=============================================="
echo ""

# Verificar se credenciais AWS estão configuradas
if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "❌ Erro: Credenciais AWS não configuradas"
    echo ""
    echo "Configure as variáveis de ambiente antes de executar:"
    echo "  export AWS_ACCESS_KEY_ID=<sua-access-key>"
    echo "  export AWS_SECRET_ACCESS_KEY=<sua-secret-key>"
    echo "  export AWS_REGION=sa-east-1"
    echo "  export AWS_S3_BUCKET=brewer-fotos"
    echo ""
    echo "Ou carregue de um arquivo .env:"
    echo "  source .env"
    echo ""
    exit 1
fi

# Configurar região e bucket (se não estiverem definidos)
export AWS_REGION=${AWS_REGION:-sa-east-1}
export AWS_S3_BUCKET=${AWS_S3_BUCKET:-brewer-fotos}

# Ativar profile de produção
export SPRING_PROFILES_ACTIVE=prod

echo "✅ Variáveis de ambiente configuradas:"
echo "   AWS_REGION: $AWS_REGION"
echo "   AWS_S3_BUCKET: $AWS_S3_BUCKET"
echo "   SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"
echo ""

# Verificar conectividade S3
echo "🔍 Verificando acesso ao bucket S3..."
if aws s3 ls s3://$AWS_S3_BUCKET/ > /dev/null 2>&1; then
    echo "✅ Bucket S3 acessível"
else
    echo "❌ Erro: Não foi possível acessar o bucket S3"
    echo "   Bucket: s3://$AWS_S3_BUCKET"
    echo "   Região: $AWS_REGION"
    echo ""
    echo "Verifique:"
    echo "  1. Credenciais AWS corretas"
    echo "  2. Bucket existe na região especificada"
    echo "  3. Permissões IAM do usuário"
    echo ""
    exit 1
fi

echo ""
echo "🏗️  Compilando aplicação..."
mvn clean compile -q

echo ""
echo "▶️  Iniciando aplicação..."
echo "   Profile ativo: PROD (usando S3)"
echo "   Bucket: s3://$AWS_S3_BUCKET"
echo "   Região: $AWS_REGION"
echo ""

mvn spring-boot:run
