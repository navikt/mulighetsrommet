import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";
import { useGetAvtaleIdFromUrlOrThrow } from "../../../hooks/useGetAvtaleIdFromUrl";

export function useMineAvtalenotater() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();

  return useQuery({
    queryKey: QueryKeys.mineAvtalenotater(avtaleId!!),
    queryFn: () =>
      mulighetsrommetClient.avtaleNotater.getMineAvtaleNotater({
        avtaleId: avtaleId!,
      }),
  });
}
