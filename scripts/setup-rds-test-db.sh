#!/bin/bash

# Script para criar o banco de dados de testes no RDS
# Requer Docker instalado

set -e

echo "🚀 Criando banco de dados de testes no AWS RDS..."
echo ""

# Variáveis
RDS_HOST="brewer-db.clhydspk2fa7.sa-east-1.rds.amazonaws.com"
RDS_USER="admin"
RDS_PASS="BrewerAdmin2024"
TEST_DB="brewer_test"

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