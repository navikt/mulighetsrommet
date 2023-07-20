import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../clients";
import { QueryKeys } from "../../QueryKeys";
import { useGetAvtaleIdFromUrl } from "../../../hooks/useGetAvtaleIdFromUrl";
import invariant from "tiny-invariant";

export function useMineAvtalenotater() {
  const avtaleId = useGetAvtaleIdFromUrl();
  invariant(avtaleId, "AvtaleId er ikke satt");

  return useQuery(
    QueryKeys.mineAvtalenotater(avtaleId!!),
    () =>
      mulighetsrommetClient.avtaleNotater.getMineAvtaleNotater({
        avtaleId: avtaleId!,
      }),
    {},
  );
}
