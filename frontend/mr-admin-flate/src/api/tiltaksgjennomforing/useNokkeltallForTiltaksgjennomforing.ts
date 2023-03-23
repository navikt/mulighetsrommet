import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useNokkeltallForTiltaksgjennomforing() {
  const { tiltaksgjennomforingId } = useParams<{
    tiltaksgjennomforingId: string;
  }>();

  if (!tiltaksgjennomforingId) {
    throw new Error("Fant ingen tiltaksgjennomføring-id i URL");
  }

  return useQuery(
    QueryKeys.nokkeltallTiltaksgjennomforing(tiltaksgjennomforingId),
    () =>
      mulighetsrommetClient.tiltaksgjennomforing.getNokkeltallForTiltaksgjennomforingWithId(
        {
          id: tiltaksgjennomforingId,
        }
      ),
    { staleTime: 1000 }
  );
}
