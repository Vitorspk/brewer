# Brewer API - Postman Test Collection

Complete Postman test collection for the Brewer Brewery Management System API.

## 📋 Contents

- **Brewer-API.postman_collection.json** - Complete API collection with 40+ endpoints
- **Development.postman_environment.example.json** - Development environment template
- **Production.postman_environment.json** - Production environment template

## ⚙️ Prerequisites

Before using the Postman collection, ensure you have:

### Required Software
- **Postman Desktop App** (v10.0.0 or later) or **Postman CLI (Newman)**
  - Download: [https://www.postman.com/downloads/](https://www.postman.com/downloads/)
  - Newman CLI: `npm install -g newman`

### Application Setup
- **Brewer application running** on `http://localhost:8080` (or configured URL)
- **MySQL database** initialized with required schema and seed data
- **Admin account** created with appropriate credentials

### Environment Configuration
1. Copy `Development.postman_environment.example.json` to `Development.postman_environment.json`
2. Configure credentials in your local copy:
   ```json
   {
     "key": "admin_password",
     "value": "your_admin_password",
     "type": "secret"
   },
   {
     "key": "test_user_password",
     "value": "your_test_password",
     "type": "secret"
   }
   ```
3. **Never commit** `Development.postman_environment.json` or `Production.postman_environment.json` with real credentials

## 🚀 Quick Start

### 1. Import Collection

1. Open Postman
2. Click **Import** button
3. Select `Brewer-API.postman_collection.json`
4. Collection will appear in your Collections sidebar

### 2. Import Environment

1. Click the gear icon (⚙️) in top right corner → **Manage Environments**
2. Click **Import** button
3. Select environment file:
   - `Development.postman_environment.json` for local testing
   - `Production.postman_environment.json` for production
4. Click on the imported environment to activate it

### 3. Run Tests

#### Option A: Manual Testing
1. Select an endpoint from the collection
2. Click **Send**
3. View response and test results

#### Option B: Collection Runner
1. Right-click on "Brewer API - Complete Test Suite"
2. Select **Run collection**
3. Configure runner options:
   - Iterations: 1
   - Delay: 100ms between requests
4. Click **Run Brewer API**
5. View test results summary

## 📁 Collection Structure

```
Brewer API
├── Authentication
│   ├── Login
│   └── Logout
├── Beers (Cervejas)
│   ├── Create Beer - Form View
│   ├── Create Beer
│   ├── Search Beers - Paginated
│   └── Search Beers - JSON (Autocomplete)
├── Customers (Clientes)
│   ├── Create Customer - Form View
│   ├── Create Customer (Individual - CPF)
│   ├── Create Customer (Company - CNPJ)
│   ├── Search Customers - Paginated
│   └── Search Customers - JSON (Autocomplete)
├── Users (Usuarios)
│   ├── Create User - Form View
│   ├── Create User
│   ├── Search Users - Paginated
│   └── Update User Status
├── Beer Styles (Estilos)
│   ├── Create Style - Form View
│   ├── Create Style (Form)
│   ├── Create Style (JSON - Modal)
│   └── Search Styles - Paginated
├── Cities (Cidades)
│   ├── Create City - Form View
│   ├── Create City
│   ├── Get Cities by State (JSON - Cached)
│   └── Search Cities - Paginated
└── Actuator - Monitoring
    ├── Health Check (Public)
    ├── Health Check - Detailed
    ├── Liveness Probe (Kubernetes)
    ├── Readiness Probe (Kubernetes)
    ├── Application Info (Public)
    ├── Metrics List (Requires ADMIN)
    ├── JVM Memory Metrics (Requires ADMIN)
    └── HTTP Requests Metrics (Requires ADMIN)
```

## 🔐 Authentication

### Session-Based Authentication

The Brewer API uses Spring Security with session-based authentication.

#### Login Flow

1. **Send Login Request**:
   ```
   POST {{base_url}}/login
   Content-Type: application/x-www-form-urlencoded

   username={{admin_email}}
   password={{admin_password}}
   ```

2. **Extract Session Cookie**:
   - The login request automatically extracts the `JSESSIONID` cookie
   - Stored in environment variable `sessionCookie`

3. **Authenticated Requests**:
   - All subsequent requests automatically include the session cookie
   - Global pre-request script handles cookie injection

#### Credentials Configuration

Configure your admin credentials in the environment file:
- **Email**: Set in `admin_email` environment variable
- **Password**: Set in `admin_password` environment variable (secure type)

Refer to the **Prerequisites** section for environment setup instructions.

### Authorization

Some endpoints require specific roles:

| Endpoint | Required Role | Description |
|----------|--------------|-------------|
| `/usuarios/**` | `CADASTRAR_USUARIO` | User management |
| `/cidades/nova` | `CADASTRAR_CIDADE` | City registration |
| `/actuator/metrics` | `ADMIN` | Metrics access |
| `/actuator/health` (detailed) | Authenticated | Detailed health info |

## 🧪 Automated Tests

The collection includes comprehensive automated tests:

### Test Scripts

Each request includes test scripts that validate:

1. **HTTP Status Codes**: Ensures correct response codes (200, 302, etc.)
2. **Response Structure**: Validates JSON structure and required fields
3. **Response Time**: Checks performance (< 5 seconds global, < 500ms for health checks)
4. **Business Logic**: Validates specific application behavior
5. **Session Management**: Verifies authentication state

### Example Test Results

```javascript
// ✓ Status code is 200
// ✓ Response is JSON array
// ✓ Each beer has required fields
// ✓ Response time is acceptable
```

### Running Test Suites

#### 1. Full Collection Test
```bash
# Using Newman (Postman CLI)
newman run Brewer-API.postman_collection.json \
  -e Development.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export ./test-results.html
```

#### 2. Specific Folder Test
```bash
# Test only Actuator endpoints
newman run Brewer-API.postman_collection.json \
  -e Development.postman_environment.json \
  --folder "Actuator - Monitoring"
```

#### 3. CI/CD Integration
```bash
# Run in CI pipeline with JSON reporter
newman run Brewer-API.postman_collection.json \
  -e Production.postman_environment.json \
  --reporters cli,json \
  --reporter-json-export ./test-results.json \
  --suppress-exit-code
```

## 📊 Environment Variables

### Development Environment

| Variable | Value | Description |
|----------|-------|-------------|
| `base_url` | `http://localhost:8080` | Application base URL |
| `admin_email` | `admin@brewer.com` | Admin login email |
| `admin_password` | *(configure)* | Admin password (secret) |
| `test_user_password` | *(configure)* | Password for test user creation (secret) |
| `sessionCookie` | *(auto-set)* | Session cookie from login |
| `last_style_id` | *(auto-set)* | Last created style ID |

### Production Environment

| Variable | Value | Description |
|----------|-------|-------------|
| `base_url` | `https://your-production-url.com` | Production URL |
| `admin_email` | *(configure)* | Production admin email |
| `admin_password` | *(configure)* | Production admin password (secret) |
| `test_user_password` | *(configure)* | Password for test user creation (secret) |
| `sessionCookie` | *(auto-set)* | Session cookie from login |
| `last_style_id` | *(auto-set)* | Last created style ID |

⚠️ **Security Note**: Never commit production credentials. Use Postman's secure storage or environment variables.

## 🔧 Common Workflows

### Workflow 1: Create Complete Beer Entry

1. **Login** (`Authentication > Login`)
2. **Create Style** (`Beer Styles > Create Style (JSON - Modal)`)
   - Extracts `last_style_id` automatically
3. **Create Beer** (`Beers > Create Beer`)
   - Use `{{last_style_id}}` in `estilo.codigo` field
4. **Search Beer** (`Beers > Search Beers - Paginated`)
   - Verify beer appears in results

### Workflow 2: Customer Registration

1. **Login** (`Authentication > Login`)
2. **Create Individual Customer** (`Customers > Create Customer (Individual - CPF)`)
   - Uses valid CPF format (11 digits)
3. **Search Customer** (`Customers > Search Customers - Paginated`)
   - Verify customer appears in results

### Workflow 3: Health Check Monitoring

1. **Basic Health** (`Actuator > Health Check (Public)`)
   - No authentication required
   - Quick status check
2. **Detailed Health** (`Actuator > Health Check - Detailed`)
   - Login first for detailed components
3. **Kubernetes Probes**
   - `Actuator > Liveness Probe`
   - `Actuator > Readiness Probe`
4. **Metrics** (`Actuator > Metrics List`)
   - Requires ADMIN role
   - Browse available metrics

## 📝 Request Examples

### Create Beer (JSON Body)

```http
POST {{base_url}}/cervejas/novo
Content-Type: application/x-www-form-urlencoded

sku=IPA001
nome=Imperial IPA
descricao=Strong hoppy beer
estilo.codigo=1
sabor=FORTE
origem=NACIONAL
valor=15.50
comissao=7.00
quantidadeEstoque=50
```

### Search Beers (With Filters)

```http
GET {{base_url}}/cervejas?page=0&size=10&nome=IPA&sabor=FORTE&origem=NACIONAL
```

### Create Style (JSON for Modal)

```http
POST {{base_url}}/estilos
Content-Type: application/json

{
  "nome": "New Style Name"
}
```

### Get Cities by State (Cached)

```http
GET {{base_url}}/cidades?estado=1
Content-Type: application/json
```

## 🐛 Troubleshooting

### Issue: "401 Unauthorized" on Protected Endpoints

**Solution**:
1. Run `Authentication > Login` first
2. Verify `sessionCookie` is set in environment variables
3. Check that global pre-request script is enabled

### Issue: "403 Forbidden" on Admin Endpoints

**Solution**:
1. Ensure you're logged in with admin account (`admin@brewer.com`)
2. Verify user has `ADMIN` or required role in database
3. Check Spring Security configuration

### Issue: "Session Expired"

**Solution**:
1. Sessions timeout after 30 minutes of inactivity
2. Run `Authentication > Login` again
3. Continue with your requests

### Issue: Validation Errors

**Solution**:
1. Check request body format matches expected fields
2. Verify required fields are present:
   - Beer: `sku`, `nome`, `estilo`, `sabor`, `origem`, `valor`
   - Customer: `nome`, `tipoPessoa`, `cpfOuCnpj`, `email`
   - User: `nome`, `email`, `senha`, `confirmacaoSenha`, `grupos`
3. Validate data formats:
   - CPF: 11 digits
   - CNPJ: 14 digits
   - Email: valid email format

## 🔗 Related Documentation

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Postman Learning Center](https://learning.postman.com/)
- [Newman CLI Documentation](https://learning.postman.com/docs/running-collections/using-newman-cli/command-line-integration-with-newman/)

## 📞 Support

For issues or questions:
- Check the main project [README.md](../README.md)
- Review Spring Security configuration in `SecurityConfig.java`
- Check controller implementations in `src/main/java/com/algaworks/brewer/controller/`

---

**Built with ❤️ for Brewer Brewery Management System**

*Last Updated: December 2025*