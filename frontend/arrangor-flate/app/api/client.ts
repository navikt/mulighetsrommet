import { createClient, createConfig, type ClientOptions } from "api-client/client";

export const queryClient = createClient(
  createConfig<ClientOptions>({
    baseUrl: "/api-proxy",
  }),
);

queryClient.interceptors.request.use((request) => {
  request.headers.set("Accept", "application/json");
  return request;
});
