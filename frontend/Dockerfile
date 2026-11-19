FROM node:24-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html vite.config.ts tsconfig*.json ./
COPY public ./public
COPY src ./src
ARG VITE_DEMO_LOGIN_ENABLED=false
ENV VITE_DEMO_LOGIN_ENABLED=${VITE_DEMO_LOGIN_ENABLED}
RUN npm run build

FROM nginx:1.28-alpine AS runtime

COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

ENV PORT=80
ENV BACKEND_URL=http://backend:8080
ENV BACKEND_HOST=backend
ENV NGINX_ENVSUBST_FILTER=^(PORT|BACKEND_URL|BACKEND_HOST)$

EXPOSE 80
