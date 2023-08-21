import { useQuery } from "@tanstack/react-query";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useAvtale(overstyrAvtaleId?: string) {
  const avtaleIdFromUrl = useGetAvtaleIdFromUrl();
  const avtaleId = overstyrAvtaleId ?? avtaleIdFromUrl;

  const query = useQuery(
    QueryKeys.avtale(avtaleId!!),
    () => mulighetsrommetClient.avtaler.getAvtale({ id: avtaleId! }),
    { enabled: !!avtaleId },
  );

  return {
    ...query,
    isLoading: !!avtaleId && query.isLoading, // https://github.com/TanStack/query/issues/3584
  };
}
