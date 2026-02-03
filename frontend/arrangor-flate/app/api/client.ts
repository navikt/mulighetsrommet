import { createClient, createConfig, type ClientOptions } from "api-client/client";
import { v4 as uuidv4 } from "uuid";

export const queryClient = createClient(
  createConfig<ClientOptions>({
    baseUrl: "/api-proxy",
  }),
);

queryClient.interceptors.request.use((request) => {
  request.headers.set("Accept", "application/json");
  request.headers.set("Nav-Call-Id", uuidv4());
  return request;
});
