import { useQuery } from "@tanstack/react-query";
import { Utkast } from "mulighetsrommet-api-client";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useMineUtkast(utkasttype: Utkast.type) {
  const avtaleId = useGetAvtaleIdFromUrl();

  return useQuery(QueryKeys.mineUtkast(avtaleId, utkasttype), () =>
    mulighetsrommetClient.utkast.getMineUtkast({
      utkasttype,
      avtaleId,
    }),
  );
}
