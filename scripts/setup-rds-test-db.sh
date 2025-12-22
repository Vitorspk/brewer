#!/bin/bash

# Script para criar o banco de dados de testes no RDS
# Requer Docker instalado e variáveis de ambiente configuradas

set -e

echo "🚀 Criando banco de dados de testes no AWS RDS..."
echo ""

# Variáveis de ambiente - DEVEM ser configuradas antes de executar
RDS_HOST="${RDS_HOST:-}"
RDS_USER="${RDS_USER:-admin}"
RDS_PASS="${RDS_PASSWORD:-}"
TEST_DB="${TEST_DB_NAME:-brewer_test}"

# Validar variáveis obrigatórias
if [ -z "$RDS_HOST" ] || [ -z "$RDS_PASS" ]; then
    echo "❌ ERRO: Variáveis de ambiente não configuradas!"
    echo ""
    echo "Configure as seguintes variáveis antes de executar:"
    echo "  export RDS_HOST='your-rds-endpoint.rds.amazonaws.com'"
    echo "  export RDS_USER='admin'"
    echo "  export RDS_PASSWORD='your-secure-password'"
    echo "  export TEST_DB_NAME='brewer_test'"
    echo ""
    echo "Ou carregue do arquivo .env.rds:"
    echo "  source .env.rds"
    exit 1
fi

echo "📊 Endpoint: $RDS_HOST"
echo "👤 Usuário: $RDS_USER"
echo "🗄️  Banco de testes: $TEST_DB"
echo ""

# Verificar se Docker está instalado
if ! command -v docker &> /dev/null; then
    echo "❌ Docker não está instalado!"
    echo "   Instale Docker ou use MySQL client diretamente:"
    echo "   mysql -h $RDS_HOST -u $RDS_USER -p$RDS_PASS -e \"CREATE DATABASE IF NOT EXISTS $TEST_DB;\""
    exit 1
fi

echo "⏳ Conectando ao RDS e criando banco de testes..."

# Criar banco de testes
docker run --rm mysql:8.0 mysql \
  -h "$RDS_HOST" \
  -u "$RDS_USER" \
  -p"$RDS_PASS" \
  -e "CREATE DATABASE IF NOT EXISTS $TEST_DB;"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Banco de testes '$TEST_DB' criado com sucesso!"
    echo ""

    # Listar bancos
    echo "📋 Bancos de dados disponíveis:"
    docker run --rm mysql:8.0 mysql \
      -h "$RDS_HOST" \
      -u "$RDS_USER" \
      -p"$RDS_PASS" \
      -e "SHOW DATABASES;"
else
    echo ""
    echo "❌ Erro ao criar banco de testes!"
    exit 1
fi

echo ""
echo "🎉 Configuração completa! Você pode rodar os testes agora."