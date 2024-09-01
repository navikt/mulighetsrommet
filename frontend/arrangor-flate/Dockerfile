FROM node:20-bullseye-slim AS base

# Setup production node_modules
FROM base AS production-deps

WORKDIR /app

ADD package.json /app/package.json

RUN --mount=type=secret,id=NODE_AUTH_TOKEN \
    NODE_AUTH_TOKEN=$(cat /run/secrets/NODE_AUTH_TOKEN) \
    npm install

FROM base

ENV PORT="3000"

WORKDIR /app

COPY --from=production-deps /app/node_modules/ /app/node_modules/
COPY node_modules/ /app/node_modules/
COPY build/ /app/build/
COPY ./server.js /app/server.js
COPY ./package.json /app/package.json

EXPOSE 3000
