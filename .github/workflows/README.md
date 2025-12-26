# CI/CD Workflows

## CI - Unit Tests (`ci.yml`)

Pipeline de integração contínua que executa automaticamente todos os testes unitários do projeto.

### Quando executa

- **Push**: Em branches `master`, `main`, `develop`, e branches de features/fixes (`fix/**`, `feature/**`)
- **Pull Request**: Para branches `master`, `main`, e `develop`

### O que faz

1. **Setup do ambiente**:
   - Configura Java 17 (Temurin)
   - Inicializa MySQL 8.0 em container para testes
   - Cache de dependências Maven

2. **Execução de testes**:
   - Aguarda MySQL estar pronto
   - Executa `mvn clean test` com perfil de teste
   - Gera relatórios de cobertura com JaCoCo

3. **Relatórios**:
   - Gera relatório Surefire com resultados dos testes
   - Faz upload dos resultados como artefatos (30 dias de retenção)
   - Publica resumo no GitHub Actions

### Configuração do MySQL

O pipeline usa um MySQL 8.0 em container com as seguintes configurações:

```yaml
Database: brewer_test
User: test
Password: test
Port: 3307 (mapeado para 3306 do container)
```

### Visualizando Resultados

Após a execução:

1. Acesse a aba "Actions" no GitHub
2. Selecione o workflow "CI - Unit Tests"
3. Clique na execução desejada
4. Veja o resumo dos testes na seção "Summary"
5. Baixe os artefatos "test-results" e "test-coverage" para análise detalhada

### Cobertura de Código

O JaCoCo gera relatórios de cobertura em:
- `target/site/jacoco/index.html` (relatório HTML)
- `target/jacoco.exec` (dados binários)

### Rodando Localmente

Para executar os testes localmente:

```bash
# Iniciar banco de dados de teste
docker run -d \
  --name mysql-test \
  -e MYSQL_ROOT_PASSWORD=root_password \
  -e MYSQL_DATABASE=brewer_test \
  -e MYSQL_USER=test \
  -e MYSQL_PASSWORD=test \
  -p 3307:3306 \
  mysql:8.0

# Aguardar MySQL iniciar
until mysqladmin ping -h 127.0.0.1 -P 3307 --silent; do
  echo 'Waiting for MySQL...'
  sleep 2
done

# Executar testes
mvn clean test

# Visualizar relatório de cobertura
open target/site/jacoco/index.html

# Parar e remover container
docker stop mysql-test && docker rm mysql-test
```

### Métricas de Qualidade

O pipeline garante:
- ✅ Todos os testes unitários passam
- ✅ Sem erros de compilação
- ✅ Relatórios de cobertura gerados
- ✅ Artefatos preservados para análise
# CI/CD Test
