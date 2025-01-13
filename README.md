# Dating App Authentication Module

## Overview
This module handles authentication for the dating app using Keycloak as the Identity and Access Management (IAM) solution. It supports multiple authentication methods:
- WeChat OAuth
- Apple Sign-in
- Phone number verification

## Architecture
The authentication flow is implemented using:
- Spring Boot (Backend)
- Keycloak (IAM)
- Identity Providers:
  - WeChat
  - Apple
  - Custom Phone Authentication

## Authentication Flows
1. **WeChat Login/Registration**
   - User initiates WeChat login
   - Redirects to WeChat OAuth
   - Returns to application with auth code
   - Creates/Updates user profile
   - Issues JWT token

2. **Apple Sign-in**
   - User initiates Apple sign-in
   - Validates Apple identity token
   - Creates/Updates user profile
   - Issues JWT token

3. **Phone Authentication**
   - User enters phone number
   - System sends OTP
   - User verifies OTP
   - Creates/Updates user profile
   - Issues JWT token

## Setup Instructions
1. Install and Configure Keycloak
   ```bash
   # Run Keycloak (example using Docker)
   docker run -p 8180:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:24.0.1 start-dev
   ```

2. Realm Configuration
   - Create new realm: `dating-app`
   - Create client: `dating-app-client`
     - Access Type: `confidential`
     - Valid Redirect URIs: `<your-app-urls>`
     - Web Origins: `<your-app-urls>`

3. Identity Provider Configuration
   
   a. WeChat Configuration:
   - Add Identity Provider → WeChat
   - Configure:
     - Client ID: `${WECHAT_APP_ID}`
     - Client Secret: `${WECHAT_APP_SECRET}`
     - Default Scopes: `snsapi_userinfo`
     - User Info URL: `https://api.weixin.qq.com/sns/userinfo`

   b. Apple Configuration:
   - Add Identity Provider → Apple
   - Configure:
     - Client ID: `${APPLE_CLIENT_ID}`
     - Team ID: `${APPLE_TEAM_ID}`
     - Key ID: `${APPLE_KEY_ID}`
     - Private Key: `${APPLE_PRIVATE_KEY}`
     - Default Scopes: `name email`

   c. Phone Authentication Configuration:
   - Create Authentication Flow:
     - Copy browser flow
     - Add "Phone Authentication" execution
     - Set requirement to "REQUIRED"

4. User Federation (Optional)
   - Configure user storage if needed

5. Environment Variables
   ```properties
   # Keycloak
   KEYCLOAK_URL=http://localhost:8180
   KEYCLOAK_REALM=dating-app
   KEYCLOAK_CLIENT_ID=dating-app-client
   KEYCLOAK_CLIENT_SECRET=your-client-secret

   # WeChat
   WECHAT_APP_ID=your-wechat-app-id
   WECHAT_APP_SECRET=your-wechat-app-secret

   # Apple
   APPLE_TEAM_ID=your-team-id
   APPLE_KEY_ID=your-key-id
   APPLE_PRIVATE_KEY=your-private-key
   APPLE_CLIENT_ID=your-client-id

   # Alibaba Cloud SMS
   ALIBABA_ACCESS_KEY_ID=your-access-key
   ALIBABA_ACCESS_KEY_SECRET=your-secret-key
   ALIBABA_SMS_SIGN_NAME=your-sign-name
   ALIBABA_SMS_TEMPLATE_CODE=your-template-code

   # Redis
   REDIS_HOST=localhost
   REDIS_PORT=6379
   REDIS_PASSWORD=your-redis-password
   ```

6. Run the Application
   ```bash
   ./mvnw spring-boot:run
   ```

## API Endpoints
- POST /api/auth/wechat
- POST /api/auth/apple
- POST /api/auth/phone/send-otp
- POST /api/auth/phone/verify
- POST /api/auth/refresh
- POST /api/auth/logout 