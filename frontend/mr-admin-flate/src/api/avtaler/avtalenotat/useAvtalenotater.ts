import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../../QueryKeys";
import { mulighetsrommetClient } from "../../clients";
import { useGetAvtaleIdFromUrl } from "../../../hooks/useGetAvtaleIdFromUrl";
import invariant from "tiny-invariant";

export function useAvtalenotater() {
  const avtaleId = useGetAvtaleIdFromUrl();
  invariant(avtaleId, "AvtaleId er ikke satt i URL");
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
