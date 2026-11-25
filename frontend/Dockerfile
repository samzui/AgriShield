FROM node:22-alpine AS build
WORKDIR /app
COPY package.json .npmrc ./
RUN for i in 1 2 3 4 5; do npm install --legacy-peer-deps --no-audit --no-fund && exit 0; echo "npm install attempt $i failed; retrying..."; sleep 5; done; exit 1
COPY . .
RUN npm run build
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
