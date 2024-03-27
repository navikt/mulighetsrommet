import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";
import { useGetAvtaleIdFromUrlOrThrow } from "../../../hooks/useGetAvtaleIdFromUrl";

export function useAvtalenotater() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();

  return useQuery({
    queryKey: QueryKeys.avtalenotater(avtaleId),
    queryFn: () =>
      mulighetsrommetClient.avtaleNotater.getNotaterForAvtale({
        avtaleId,
      }),
    enabled: !!avtaleId,
  });
}
