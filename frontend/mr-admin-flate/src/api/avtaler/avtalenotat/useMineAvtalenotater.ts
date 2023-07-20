import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";
import { QueryKeys } from "../../QueryKeys";
import { useGetAvtaleIdFromUrl } from "../../../hooks/useGetAvtaleIdFromUrl";

export function useMineAvtalenotater() {
  const avtaleId = useGetAvtaleIdFromUrl();
  const enabled = !!avtaleId;

  return useQuery(
    QueryKeys.mineAvtalenotater(avtaleId!!),
    () =>
      mulighetsrommetClient.avtaleNotater.getMineAvtaleNotater({
        avtaleId: avtaleId!,
      }),
    { enabled },
  );
}
