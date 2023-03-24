import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAlleTiltakstyper() {
  return useQuery(QueryKeys.alleTiltakstyper(), () =>
    mulighetsrommetClient.tiltakstyper.getTiltakstyper({
      search: undefined,
      tiltakstypestatus: undefined,
      tiltakstypekategori: undefined,
      sort: undefined,
      page: 1,
      size: 1000,
    })
  );
}
