FROM node:24.16.0-alpine AS base

WORKDIR /app

COPY ./package*.json .

# Install only the root dependencies to leverage Docker cache
RUN npm install --workspaces=false

COPY ./tsconfig.base.json .

COPY ./vite.config.ts .

COPY ./common ./common
