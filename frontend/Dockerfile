FROM node:22.16-alpine AS base

WORKDIR /app

COPY ./package*.json .

RUN npm install

COPY ./tsconfig.base.json .

COPY ./vite.config.ts .
