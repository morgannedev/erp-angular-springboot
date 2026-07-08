# Construir el frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN cp tsconfig.json tsconfig.app.json
RUN npm run build -- --configuration production
# 🔽 DEPURACIÓN DEL FRONTEND 🔽
RUN echo "=== CONTENIDO DE /app/frontend/dist ===" && \
    ls -la /app/frontend/dist && \
    echo "=== FIN ==="

# Construir el backend
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-build
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Imagen final
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar el JAR del backend
COPY --from=backend-build /app/backend/target/*.jar app.jar

# Copiar el frontend construido
COPY --from=frontend-build /app/frontend/dist/algedro-frontend /app/static

# 🔽 DEPURACIÓN DEL DIRECTORIO ESTÁTICO 🔽
RUN echo "=== CONTENIDO DE /app/static ===" && \
    ls -la /app/static || echo "El directorio /app/static está vacío o no existe" && \
    echo "=== FIN ==="

# Instalar Nginx
RUN apk add --no-cache nginx

# Configurar Nginx
RUN echo 'server { \
    listen 80; \
    server_name localhost; \
    root /app/static; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://localhost:8080/api/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
    } \
    error_log /var/log/nginx/error.log debug; \
}' > /etc/nginx/http.d/default.conf

# Variables de entorno
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Asegurar permisos
RUN mkdir -p /app/static && \
    chown -R nginx:nginx /app/static && \
    chmod -R 755 /app/static

EXPOSE 80

# Script de inicio mejorado
RUN echo '#!/bin/sh' > /start.sh && \
    echo 'echo "=== Iniciando Nginx ==="' >> /start.sh && \
    echo 'nginx -g "daemon off;" &' >> /start.sh && \
    echo 'echo "=== Iniciando Spring Boot ==="' >> /start.sh && \
    echo 'java -jar app.jar' >> /start.sh && \
    chmod +x /start.sh

CMD ["/start.sh"]