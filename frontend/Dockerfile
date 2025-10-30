FROM node:22.21.1-alpine AS base

WORKDIR /app

COPY ./package*.json .

# Install only the root dependencies to leverage Docker cache
RUN npm install --workspaces=false

COPY ./tsconfig.base.json .

COPY ./vite.config.ts .
