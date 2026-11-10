FROM node:22-bookworm-slim
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
EXPOSE 5173
CMD ["node", "node_modules/vite/bin/vite.js", "--host", "0.0.0.0", "--port", "5173", "--strictPort"]
