FROM node:20-bullseye-slim AS base

ENV NODE_ENV production
ENV PORT="3000"

WORKDIR /app

COPY .npmrc package*.json /app/

RUN --mount=type=secret,id=NODE_AUTH_TOKEN \
    && NODE_AUTH_TOKEN=$(cat /run/secrets/NODE_AUTH_TOKEN) \
    && npm install --no-audit --ignore-scripts

COPY build/ /app/build/
COPY ./server.js /app/server.js

EXPOSE 3000
CMD ["npm", "run", "start"]
