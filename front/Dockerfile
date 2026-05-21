# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS deps
RUN corepack enable
WORKDIR /app
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

FROM node:22-alpine AS build
RUN corepack enable
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_API_BASE=""
ENV NEXT_PUBLIC_API_BASE=$NEXT_PUBLIC_API_BASE \
    NEXT_TELEMETRY_DISABLED=1
RUN pnpm build \
    && cp -r public .next/standalone/ \
    && cp -r .next/static .next/standalone/.next/

FROM node:22-alpine AS runtime
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
ENV NODE_ENV=production \
    PORT=3000 \
    HOSTNAME=0.0.0.0 \
    NEXT_TELEMETRY_DISABLED=1
COPY --from=build --chown=app:app /app/.next/standalone ./
USER app
EXPOSE 3000
CMD ["node","server.js"]
