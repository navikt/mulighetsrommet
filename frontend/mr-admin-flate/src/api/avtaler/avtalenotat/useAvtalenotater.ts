import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../../QueryKeys";
import { mulighetsrommetClient } from "../../clients";
import { useGetAvtaleIdFromUrl } from "../../../hooks/useGetAvtaleIdFromUrl";

export function useAvtalenotater() {
  const avtaleId = useGetAvtaleIdFromUrl();
  if (!avtaleId) throw new Error("AvtaleId er ikke satt i URL");
  return useQuery(
    QueryKeys.avtalenotater(avtaleId),
    () =>
      mulighetsrommetClient.avtaleNotater.getNotaterForAvtale({
        avtaleId,
      }),
    {
      enabled: !!avtaleId,
    },
  );
}
