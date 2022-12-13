import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforingById() {
  const { tiltaksgjennomforingId } = useParams<{
    tiltaksgjennomforingId: string;
  }>();

  if (!tiltaksgjennomforingId) {
    throw new Error("Fant ingen tiltaksgjennomførings-id i URL");
  }

  console.log("Denne bør dukke opp i Network-taben");
  mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforingMedTiltakstype(
    { id: tiltaksgjennomforingId }
  );

  return useQuery(QueryKeys.tiltaksgjennomforing(tiltaksgjennomforingId), () =>
    mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforingMedTiltakstype(
      {
        id: tiltaksgjennomforingId,
      }
    )
  );
}
