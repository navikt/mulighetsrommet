import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforingById() {
  const { tiltaksgjennomforingId } = useParams<{
    tiltaksgjennomforingId: string;
  }>();

  return useQuery(
    QueryKeys.tiltaksgjennomforing(tiltaksgjennomforingId),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforing({
        id: tiltaksgjennomforingId!!,
      }),
    { enabled: !!tiltaksgjennomforingId },
  );
}
