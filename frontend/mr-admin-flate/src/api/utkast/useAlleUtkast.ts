import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useAlleUtkast(avtaleId?: string, opprettetAv?: string) {
  return useQuery(
    QueryKeys.alleUtkast(avtaleId, opprettetAv),
    () =>
      mulighetsrommetClient.utkast.getAlleUtkast({
        utkasttype: "Tiltaksgjennomforing",
        opprettetAv: opprettetAv || undefined,
        avtaleId: avtaleId!!,
      }),
    {
      enabled: !!avtaleId,
    }
  );
}
