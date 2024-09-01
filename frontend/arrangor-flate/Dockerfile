FROM node:20-bullseye-slim AS base

ENV NODE_ENV production
ENV PORT="3000"

WORKDIR /app

COPY .npmrc /.
COPY package.json /.

RUN --mount=type=secret,id=NODE_AUTH_TOKEN \
    NODE_AUTH_TOKEN=$(cat /run/secrets/NODE_AUTH_TOKEN) npm install

COPY node_modules/ /app/node_modules/
COPY build/ /app/build/
COPY ./server.js /app/server.js
COPY ./package.json /app/package.json

EXPOSE 3000
CMD ["npm", "run", "start"]
