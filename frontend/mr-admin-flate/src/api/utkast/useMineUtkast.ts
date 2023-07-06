import { useQuery } from "@tanstack/react-query";
import { Utkast } from "mulighetsrommet-api-client";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useMineUtkast(utkasttype: Utkast.type) {
  const avtaleId = useGetAvtaleIdFromUrl();
  if (!avtaleId)
    throw new Error("AvtaleID ble ikke funnet ved henting av 'Mine utkast'");
  return useQuery(
    QueryKeys.mineUtkast(avtaleId),
    () =>
      mulighetsrommetClient.utkast.getMineUtkast({
        utkasttype,
        avtaleId,
      }),
    {
      enabled: !!avtaleId,
    }
  );
}
