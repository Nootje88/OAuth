# OAuth 2.0 Authentication Template

A comprehensive Spring Boot template for implementing OAuth 2.0 authentication with multiple providers, including Google, Spotify, Apple, and SoundCloud. This template also includes email/password authentication with email verification.

## 🌟 Features

- **Multiple Authentication Methods**:
  - OAuth 2.0 integration with Google, Spotify, Apple, and SoundCloud
  - Traditional email/password authentication
  - Email verification for account activation
  - Password reset functionality

- **User Management**:
  - Role-based access control (User, Admin, Moderator, Premium)
  - Profile management with update history
  - Notification preferences
  - Theme preferences
  - Language/Internationalization support

- **Security Features**:
  - JWT authentication with refresh tokens
  - HTTP-only cookies for token storage
  - Rate limiting to prevent brute force attacks
  - Comprehensive audit logging
  - CSRF protection
  - XSS protection

- **API Documentation**:
  - Swagger/OpenAPI integration
  - Grouped API endpoints by functionality

- **Monitoring and Metrics**:
  - Spring Actuator integration
  - Micrometer metrics
  - Prometheus compatibility
  - Custom health indicators

- **DevOps Ready**:
  - Docker support with multi-stage builds
  - Environment-specific configurations (dev, test, pat, prod)
  - Azure DevOps pipeline configuration
  - Infrastructure as Code with ARM templates

## 🚀 Getting Started

> ⚠️ **Security Note**: This template follows the `.env` approach for configuration. These files will contain sensitive information and should NEVER be committed to your Git repository. The `.gitignore` file should be configured to exclude all `.env.*` files. Always create these files locally and securely share them with your team members outside of your version control system.

### Prerequisites

- Java 23 or later
- Maven 3.8 or later
- MySQL 8.0 or later
- Docker and Docker Compose (optional)

### Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/oauth-template.git
   cd oauth-template
   ```

2. **Create environment file**:
   Copy the template environment file and customize it for each environment:
   ```bash
   cp .env.template .env.dev
   # Also create for other environments as needed
   # cp .env.template .env.test
   # cp .env.template .env.pat
   # cp .env.template .env.prod
   ```
   
   > ⚠️ **IMPORTANT**: All `.env.*` files contain sensitive information and should NEVER be committed to your repository. Make sure they are included in your `.gitignore` file!
   
3. **Update environment variables**:
   Open `.env.dev` and update the following required variables:
   - `DB_USERNAME`: Your MySQL username
   - `DB_PASSWORD`: Your MySQL password
   - `DB_URL`: Your MySQL connection URL
   - `JWT_SECRET`: A strong, random secret key for JWT signing
   - `GOOGLE_CLIENT_ID`: Your Google OAuth client ID
   - `GOOGLE_CLIENT_SECRET`: Your Google OAuth client secret
   - `EMAIL_USERNAME`: Your email service username
   - `EMAIL_PASSWORD`: Your email service password

4. **Build the application**:
   ```bash
   ./mvnw clean package
   ```

5. **Run the application**:
   With Maven:
   ```bash
   ./mvnw spring-boot:run -Dspring.profiles.active=dev
   ```
   
   With Docker:
   ```bash
   docker-compose -f docker/compose/docker-compose.yml -f docker/compose/docker-compose.dev.yml up
   ```

6. **Access the application**:
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html

## 🔧 Configuration

### Environment Files

This project uses separate environment files for different deployment environments:

- `.env.dev` - Development environment
- `.env.test` - Testing environment
- `.env.pat` - Pre-production acceptance testing
- `.env.prod` - Production environment

Each environment file should be created manually based on the `.env.template` and should NOT be committed to your repository. Make sure to add the following to your `.gitignore` file:

```
# Environment files
.env
.env.dev
.env.test
.env.pat
.env.prod
```

The project already includes a basic `.gitignore` file, but you should verify that these patterns are present.

### OAuth Providers

To configure OAuth providers, you'll need to:

1. **Google OAuth**:
   - Go to [Google Developer Console](https://console.developers.google.com/)
   - Create a new project
   - Set up OAuth consent screen
   - Create OAuth credentials
   - Set authorized redirect URIs to:
     - `http://localhost:8080/login/oauth2/code/google` (for development)
     - `https://your-production-domain.com/login/oauth2/code/google` (for production)
   - Add the client ID and secret to your `.env.{environment}` file

2. **Other OAuth Providers**:
   - Uncomment and configure the relevant sections in `application.yaml`
   - Follow similar steps to create OAuth apps and obtain credentials

### Email Configuration

For email verification and password reset functionality:

1. **Gmail** (for development):
   - Enable "Less secure apps" or create an App Password
   - Update EMAIL_* variables in your `.env.{environment}` file

2. **SMTP Server** (for production):
   - Configure your SMTP server details
   - Update EMAIL_* variables in your `.env.{environment}` file

### Database Configuration

The template uses MySQL by default:

1. **Development**:
   - Create a local MySQL database
   - Update DB_* variables in your `.env.{environment}` file

2. **Production**:
   - Configure a production-grade MySQL database
   - Update DB_* variables in your `.env.{environment}` file
   - Consider securing your database connection

## 🧩 Project Customization

### Required Changes

The following changes are required to make this template your own:

1. **Package Names**:
   - Change `com.template.OAuth` to your own package structure
   - Update references in:
     - Java files
     - `pom.xml`
     - `application.yaml`

2. **Application Name and Properties**:
   - Update application name in `pom.xml`
   - Update branding in email templates
   - Update application info in `application.yaml`

3. **URLs and Domains**:
   - Update CORS allowed origins in `CorsConfig.java`
   - Update redirect URLs in OAuth provider configurations
   - Update baseUrl in environment files

4. **Default Admin Users**:
   - Modify `UserService.java` to set your admin email addresses

### Optional Customizations

1. **Additional OAuth Providers**:
   - Uncomment and configure additional providers in `application.yaml`
   - Implement the corresponding authentication flows

2. **Email Templates**:
   - Customize HTML email templates in `src/main/resources/templates/email/`

3. **Role-Based Access**:
   - Modify role definitions in `Role.java`
   - Update security restrictions in `SecurityConfig.java`

4. **API Endpoints**:
   - Customize or add API endpoints in `controller` package
   - Update OpenAPI documentation

## 🏗️ Architecture

### Packages

- `config`: Configuration classes
- `controller`: REST controllers
- `dto`: Data Transfer Objects
- `entities`: JPA entities
- `enums`: Enum definitions
- `repositories`: Spring Data JPA repositories
- `security`: Security-related classes
- `service`: Business logic services
- `validation`: Validation utilities
- `aspect`: Aspect-oriented programming components
- `filter`: HTTP filters
- `annotation`: Custom annotations
- `health`: Custom health indicators

### Database Schema

The template uses the following main entities:

- `User`: Core user entity with authentication info
- `RefreshToken`: Refresh tokens for JWT renewal
- `ProfileUpdateHistory`: User profile change history
- `AuditEvent`: Security and system audit logs

## 🚢 Deployment

### Docker Deployment

```bash
# Development environment
docker-compose -f docker/compose/docker-compose.yml -f docker/compose/docker-compose.dev.yml up -d

# Test environment
docker-compose -f docker/compose/docker-compose.yml -f docker/compose/docker-compose.test.yml up

# PAT (Pre-production) environment
docker-compose -f docker/compose/docker-compose.yml -f docker/compose/docker-compose.pat.yml up -d

# Production environment
docker-compose -f docker/compose/docker-compose.yml -f docker/compose/docker-compose-prod.yml up -d
```

### Azure Deployment

The template includes Azure DevOps pipeline configurations and infrastructure code:

1. **ARM Template**:
   - `azure/arm-template.json` defines the infrastructure
   - Run `azure/deploy-infrastructure.ps1` to provision resources

2. **Azure DevOps Pipeline**:
   - `azure-pipelines.yml` provides CI/CD configurations
   - Run `azure/setup-azure-devops-variables.ps1` to set up pipeline variables

3. **Container Registry**:
   - Use `azure/build-and-push-image.ps1` to build and push Docker images

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgements

- Spring Boot and Spring Security
- OAuth 2.0 providers
- Docker and docker-compose
- Azure DevOps

---

⚠️ **Security Notice**: Before using this template in production, ensure all security aspects are properly configured, especially:
- JWT secrets should be strong, random, and kept secure
- Database credentials should be properly protected
- OAuth client secrets should be stored securely
- Production deployments should use HTTPS exclusively
- Environment files (`.env.*`) should NEVER be committed to version control
- Use secrets management services (like Azure Key Vault, AWS Secrets Manager, or HashiCorp Vault) for production deployments
