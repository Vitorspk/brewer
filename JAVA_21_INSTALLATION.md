# Guia de Instalação do Java 21 LTS

## Problema Identificado

O Java 25 tem uma **incompatibilidade conhecida com o Mockito** que causa falhas em 53+ testes unitários.
A solução é fazer downgrade para **Java 21 LTS** (Long-Term Support).

## Por que Java 21 LTS?

- ✅ **Última versão LTS antes do Java 25**
- ✅ **Totalmente compatível com Mockito e todos os frameworks de teste**
- ✅ **Suportado até setembro de 2029**
- ✅ **Compatível com Spring Boot 3.2.1**
- ✅ **Recomendado pela Oracle para produção**

## Instruções de Instalação (macOS)

### Opção 1: Oracle JDK 21 (Recomendado)

1. **Baixar Oracle JDK 21**
   - Acesse: https://www.oracle.com/java/technologies/downloads/#java21
   - Selecione: **macOS** → **x64 DMG Installer**
   - Download: `jdk-21_macos-x64_bin.dmg`

2. **Instalar**
   ```bash
   # Abra o arquivo .dmg baixado e siga o instalador
   # O Java será instalado em: /Library/Java/JavaVirtualMachines/jdk-21.jdk
   ```

3. **Verificar Instalação**
   ```bash
   /usr/libexec/java_home -V
   # Deve mostrar ambos Java 25 e Java 21
   ```

### Opção 2: SDKMAN! (Gerenciador de Versões)

```bash
# Instalar SDKMAN!
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Instalar Java 21 LTS
sdk install java 21.0.5-oracle

# Listar versões instaladas
sdk list java
```

### Opção 3: Homebrew

```bash
# Instalar OpenJDK 21 via Homebrew
brew install openjdk@21

# Criar symlink
sudo ln -sfn /usr/local/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

## Configurar Java 21 como Padrão

### Método 1: Temporário (por sessão)

```bash
# Configurar apenas para a sessão atual do terminal
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
java -version  # Deve mostrar Java 21
```

### Método 2: Permanente (recomendado)

Adicione ao final do arquivo `~/.zshrc` (ou `~/.bash_profile` se usar bash):

```bash
# Java 21 LTS
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

Depois recarregue o terminal:
```bash
source ~/.zshrc
java -version  # Deve mostrar Java 21
```

### Método 3: Usar jenv (Gerenciador de versões Java)

```bash
# Instalar jenv
brew install jenv

# Adicionar ao ~/.zshrc
echo 'export PATH="$HOME/.jenv/bin:$PATH"' >> ~/.zshrc
echo 'eval "$(jenv init -)"' >> ~/.zshrc
source ~/.zshrc

# Adicionar versões do Java
jenv add /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
jenv add /Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home

# Configurar Java 21 como global
jenv global 21

# Verificar
jenv versions
java -version
```

## Configurar IntelliJ IDEA (se usar)

1. **File → Project Structure (⌘;)**
2. **Project Settings → Project**
   - SDK: Selecione Java 21
   - Language Level: 21
3. **Platform Settings → SDKs**
   - Clique em "+" e adicione o JDK 21
   - Caminho: `/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`

## Configurar VS Code (se usar)

Adicione ao `.vscode/settings.json`:

```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home",
      "default": true
    }
  ],
  "java.home": "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
}
```

## Verificar Instalação Completa

```bash
# 1. Verificar versão do Java
java -version
# Deve mostrar: java version "21.0.x"

# 2. Verificar JAVA_HOME
echo $JAVA_HOME
# Deve mostrar: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

# 3. Verificar compilação do Maven
cd /Users/home/Documents/workspace-bitbucket/brewer
mvn clean compile
# Deve compilar sem erros

# 4. Rodar testes
mvn test
# Agora todos os 82 testes devem passar!
```

## Desinstalar Java 25 (Opcional)

Se quiser remover completamente o Java 25:

```bash
sudo rm -rf /Library/Java/JavaVirtualMachines/jdk-25.jdk
```

## Problemas Comuns

### 1. "Command not found: java"

```bash
# Verificar se o PATH está configurado
echo $PATH | grep java

# Se não aparecer, adicione ao ~/.zshrc:
export PATH="$JAVA_HOME/bin:$PATH"
```

### 2. Maven ainda usa Java 25

```bash
# Forçar Maven a usar JAVA_HOME correto
mvn -version
# Se mostrar Java 25, execute:
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### 3. Testes ainda falhando

```bash
# Limpar cache do Maven
mvn clean
rm -rf ~/.m2/repository

# Recompilar
mvn clean install -DskipTests
mvn test
```

## Referências

- [Oracle Java 21 Downloads](https://www.oracle.com/java/technologies/downloads/#java21)
- [Spring Boot 3.2 System Requirements](https://docs.spring.io/spring-boot/docs/3.2.1/reference/html/getting-started.html#getting-started.system-requirements)
- [Mockito Java Compatibility](https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-5)
- [Java LTS Versions](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)

## Suporte

Se encontrar problemas, verifique:
1. Versão do Java: `java -version`
2. JAVA_HOME: `echo $JAVA_HOME`
3. Maven usando Java correto: `mvn -version`
4. Versões instaladas: `/usr/libexec/java_home -V`