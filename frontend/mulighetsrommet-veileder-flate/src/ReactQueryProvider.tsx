import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { ReactNode } from "react";
import { client } from "@api-client";
import { v4 as uuidv4 } from "uuid";
import { APPLICATION_NAME } from "@/constants";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      throwOnError: true,
      retry: 3,
    },
  },
});

client.setConfig({
  baseUrl: import.meta.env.VITE_MULIGHETSROMMET_API_BASE ?? "",
});

client.interceptors.request.use((request) => {
  request.headers.set("Accept", "application/json");
  request.headers.set("Nav-Call-Id", uuidv4());
  request.headers.set("Nav-Consumer-Id", APPLICATION_NAME);
  if (import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN) {
    request.headers.set(
      "Authorization",
      `Bearer ${import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN}`,
    );
  }

  return request;
});

export interface ReactQueryProviderProps {
  children: ReactNode;
}

export function ReactQueryProvider(props: ReactQueryProviderProps) {
  return (
    <QueryClientProvider client={queryClient}>
      {props.children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
