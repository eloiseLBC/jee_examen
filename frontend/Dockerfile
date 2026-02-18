# ---- Stage 1 : Build ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json .
RUN npm install
COPY . .
RUN npm run build

# ---- Stage 2 : Serve ----
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
# Config nginx pour le SPA routing (rediriger toutes les routes vers index.html)
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
