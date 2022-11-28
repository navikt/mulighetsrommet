import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltaksgjennomforing() {
  return useQuery(QueryKeys.tiltaksgjennomforinger, () =>
    mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({})
  );
}
