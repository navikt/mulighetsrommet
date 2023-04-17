import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAvtale(avtaleId?: string) {
  const enabled = !!avtaleId;

  const { data, isLoading, isError, error } = useQuery(QueryKeys.avtale(avtaleId!!), () =>
    mulighetsrommetClient.avtaler.getAvtale({ id: avtaleId!! })
    , { enabled });
  
  return {
    data,
    isLoading: isLoading && enabled, // When disabled, isLoading is for some reason true...
    isError,
    error
  }
}
