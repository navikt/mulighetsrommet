import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforingById() {
  const { tiltaksgjennomforingId } = useParams<{
    tiltaksgjennomforingId: string;
  }>();

  console.log("Inne i useTiltaksgjennomforingById for mr-admin-flate");
  if (!tiltaksgjennomforingId) {
    console.log("Fant ingen tiltaksgjennomførings-id i URL");
    throw new Error("Fant ingen tiltaksgjennomførings-id i URL");
  }

  console.log("Her bør vi begynne å fetche!");
  return useQuery(QueryKeys.tiltaksgjennomforing(tiltaksgjennomforingId), () =>
    mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforingMedTiltakstype(
      {
        id: tiltaksgjennomforingId,
      }
    )
  );
}
