import { useQuery } from "@tanstack/react-query";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useAvtale(overstyrAvtaleId?: string | null) {
  const avtaleId = overstyrAvtaleId || useGetAvtaleIdFromUrl();

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
