import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAvtalerForTiltakstype() {
  const { tiltakstypeId } = useParams();

  if (!tiltakstypeId)
    throw new Error("Kan ikke hente avtaler for tiltakstype uten id");

  return useQuery(QueryKeys.avtalerForTiltakstype(tiltakstypeId), () =>
    mulighetsrommetClient.tiltakstyper.getAvtalerForTiltakstype({
      id: tiltakstypeId,
    })
  );
}
