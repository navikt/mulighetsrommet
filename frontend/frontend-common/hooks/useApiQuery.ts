import {
  UseQueryOptions,
  UseSuspenseQueryOptions,
  UseSuspenseQueryResult,
  useQuery,
  useSuspenseQueries,
  useSuspenseQuery,
} from "@tanstack/react-query";

// Define the type of your wrapped useQuery
interface UseQueryWrapperOptions<TQueryFnData, TError, TData> extends Omit<
  UseQueryOptions<TQueryFnData, TError, TData>,
  "queryFn"
> {
  queryFn: () => Promise<{ data: TQueryFnData }>;
}

/* Siden hey-api sin fetch-client returnerer { data, error } i stedet for kun
 * data har vi denne utilitien som unwrapper så returobjektet ser likt ut som
 * useQuery.
 *
 * Synes egentlig det er litt rart at dette trengs når vi setter throwOnError
 * i hey-api (som vi gjør), kunne ikke den bare returnert data da? Kan hende
 * det er en config et sted.
 */
export function useApiQuery<TQueryFnData, TError, TData = TQueryFnData>(
  options: UseQueryWrapperOptions<TQueryFnData, TError, TData>,
) {
  const { queryFn, ...restOptions } = options;

  // Use the original useQuery, mapping the returned data to data?.data
  return useQuery<TQueryFnData, TError, TData>({
    ...restOptions,
    queryFn: async () => {
      const result = await queryFn();
      return result.data; // Extract the nested `data`
    },
  });
}

// Define the type of your wrapped useSuspenseQuery
interface UseSuspenseQueryWrapperOptions<TQueryFnData, TError, TData> extends Omit<
  UseSuspenseQueryOptions<TQueryFnData, TError, TData>,
  "queryFn"
> {
  queryFn: () => Promise<{ data: TQueryFnData }>;
}

type ApiSuspenseQueryOptions<
  TQueryFnData = unknown,
  TData = TQueryFnData,
> = UseSuspenseQueryWrapperOptions<TQueryFnData, never, TData>;

type ApiSuspenseQueryResult<T extends ApiSuspenseQueryOptions> = T extends {
  select: (data: never) => infer TData;
}
  ? UseSuspenseQueryResult<TData, never>
  : T extends ApiSuspenseQueryOptions<infer TQueryFnData>
    ? UseSuspenseQueryResult<TQueryFnData, never>
    : never;

type ApiSuspenseQueryResults<T extends readonly ApiSuspenseQueryOptions[]> = {
  [K in keyof T]: ApiSuspenseQueryResult<T[K]>;
};

export function useApiSuspenseQuery<TQueryFnData, TData = TQueryFnData>(
  options: UseSuspenseQueryWrapperOptions<TQueryFnData, never, TData>,
) {
  const { queryFn, ...restOptions } = options;

  // Use the original useSuspenseQuery, mapping the returned data to data?.data
  return useSuspenseQuery<TQueryFnData, never, TData>({
    ...restOptions,
    queryFn: async () => {
      const result = await queryFn();
      if (result.data === undefined) {
        throw new Error("Data is undefined"); // Ensure data is always defined
      }
      return result.data; // Extract the nested `data`
    },
    retry: retryServerError,
  });
}

export function useApiSuspenseQueries<T extends readonly ApiSuspenseQueryOptions[]>(options: {
  queries: T;
}): ApiSuspenseQueryResults<T> {
  const queries = options.queries.map(({ queryFn, ...queryOptions }) => ({
    ...queryOptions,
    queryFn: async () => {
      const result = await queryFn();
      if (result.data === undefined) {
        throw new Error("Data is undefined");
      }
      return result.data;
    },
    retry: retryServerError,
  }));

  return useSuspenseQueries({ queries }) as ApiSuspenseQueryResults<T>;
}

function retryServerError(failureCount: number, error: unknown): boolean {
  return (
    typeof error === "object" &&
    error !== null &&
    "status" in error &&
    typeof error.status === "number" &&
    error.status >= 500 &&
    failureCount < 3
  );
}
