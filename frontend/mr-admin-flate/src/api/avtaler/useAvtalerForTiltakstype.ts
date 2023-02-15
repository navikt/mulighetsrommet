import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { useParams } from "react-router-dom";
import { avtaleFilter } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAvtalerForTiltakstype() {
  const { tiltakstypeId } = useParams();
  const [filter] = useAtom(avtaleFilter);
  const debouncedSok = useDebounce(filter.sok, 300);

  if (!tiltakstypeId)
    throw new Error("Kan ikke hente avtaler for tiltakstype uten id");
  return useQuery(
    QueryKeys.avtalerForTiltakstype(
      tiltakstypeId,
      debouncedSok,
      filter.status,
      filter.enhet,
      filter.sortering
    ),
    () =>
      mulighetsrommetClient.avtaler.getAvtalerForTiltakstype({
        id: tiltakstypeId,
        search: debouncedSok ?? "",
        avtalestatus: filter.status,
        enhet: filter.enhet ?? "",
      })
  );
}
