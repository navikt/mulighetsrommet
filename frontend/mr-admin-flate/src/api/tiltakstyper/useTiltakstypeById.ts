import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltakstypeById() {
  const { tiltakstypeId } = useParams<{ tiltakstypeId: string }>();

  if (!tiltakstypeId) {
    throw new Error("Fant ingen tiltakstype-id i URL");
  }

  return useQuery(
    QueryKeys.tiltakstype(tiltakstypeId),
    () =>
      mulighetsrommetClient.tiltakstyper.getTiltakstypeById({
        id: tiltakstypeId!!,
      }),
    { staleTime: 1000 }
  );
}
