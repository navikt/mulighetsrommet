import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useTiltakstyper() {
  return useQuery(QueryKeys.tiltakstyper, () =>
    mulighetsrommetClient.tiltakstyper.getTiltakstyper({})
  );
}
