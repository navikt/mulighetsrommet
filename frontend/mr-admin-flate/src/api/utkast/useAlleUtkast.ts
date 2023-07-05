import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useAlleUtkast(avtaleId?: string) {
  return useQuery(
    QueryKeys.alleUtkast(avtaleId),
    () =>
      mulighetsrommetClient.utkast.getAlleUtkast({
        utkasttype: "Tiltaksgjennomforing",
        avtaleId: avtaleId!!,
      }),
    {
      enabled: !!avtaleId,
    }
  );
}
