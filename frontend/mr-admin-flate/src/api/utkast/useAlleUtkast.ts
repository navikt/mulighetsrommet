import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";

export function useAlleUtkast() {
  const avtaleId = useGetAvtaleIdFromUrl();
  if (!avtaleId) throw new Error("AvtaleId er ikke satt i URL");

  return useQuery({
    queryKey: QueryKeys.alleUtkast(avtaleId),
    queryFn: () =>
      mulighetsrommetClient.utkast.getAlleUtkast({
        utkasttype: "Tiltaksgjennomforing",
        avtaleId,
      }),
    enabled: !!avtaleId,
  });
}
