# Construir el frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build -- --configuration production

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
COPY --from=frontend-build /app/frontend/dist/frontend /app/static

# Instalar Nginx
RUN apk add --no-cache nginx

# Configurar Nginx para servir el frontend y proxy al backend
RUN echo 'server { \
    listen 80; \
    server_name localhost; \
    location / { \
        root /app/static; \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass http://localhost:8080/api/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
    } \
}' > /etc/nginx/http.d/default.conf

# Variables de entorno
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

EXPOSE 80

# Script de inicio
RUN echo '#!/bin/sh \n\
nginx -g "daemon off;" & \n\
java -jar app.jar' > /start.sh && chmod +x /start.sh

CMD ["/start.sh"]