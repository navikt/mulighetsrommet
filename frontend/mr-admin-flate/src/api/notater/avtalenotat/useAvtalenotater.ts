import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../../QueryKeys";
import { mulighetsrommetClient } from "../../clients";
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
