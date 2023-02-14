import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { useParams } from "react-router-dom";
import { avtaleFilter } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAvtalerForTiltakstype() {
  const { tiltakstypeId } = useParams();
  const [filter] = useAtom(avtaleFilter);
  // TODO Send filter for avtaler til backend

  if (!tiltakstypeId)
    throw new Error("Kan ikke hente avtaler for tiltakstype uten id");
  // TODO Må oppdatere queryKeys med filter når vi skal hente fra backend
  return useQuery(QueryKeys.avtalerForTiltakstype(tiltakstypeId), () =>
    mulighetsrommetClient.avtaler.getAvtalerForTiltakstype({
      id: tiltakstypeId,
    })
  );
}
