FROM node:20-bullseye-slim AS base

WORKDIR /app
ENV PORT="3000"

COPY node_modules/ /app/node_modules/
COPY build/ /app/build/
COPY ./server.js /app/server.js
COPY ./package.json /app/package.json

RUN npm install

EXPOSE 3000

CMD ["npm", "run", "start"]
