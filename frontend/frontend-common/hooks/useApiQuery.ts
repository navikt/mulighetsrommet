import { useQuery, UseQueryOptions, useSuspenseQuery } from "@tanstack/react-query";

// Define the type of your wrapped useQuery
interface UseQueryWrapperOptions<TQueryFnData, TError, TData>
  extends Omit<UseQueryOptions<TQueryFnData, TError, TData>, "queryFn"> {
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
interface UseSuspenseQueryWrapperOptions<TQueryFnData, TError, TData>
  extends Omit<UseQueryOptions<TQueryFnData, TError, TData>, "queryFn"> {
  queryFn: () => Promise<{ data: TQueryFnData }>;
}

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
    retry: (failureCount, response: Response) => {
      return response.status >= 500 && failureCount < 3;
    },
  });
}
