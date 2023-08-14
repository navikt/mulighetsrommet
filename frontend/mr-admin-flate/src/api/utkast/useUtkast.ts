import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useUtkast(utkastId?: string) {
  const query = useQuery(
    QueryKeys.utkast(utkastId),
    () => mulighetsrommetClient.utkast.getUtkast({ id: utkastId! }),
    { enabled: !!utkastId },
  );

  return {
    ...query,
    isLoading: !!utkastId && query.isLoading, // https://github.com/TanStack/query/issues/3584
  }
}
