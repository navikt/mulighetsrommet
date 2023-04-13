import { useQuery } from "@tanstack/react-query";
import { Tiltakstypestatus } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

interface Filter {
  tiltakstypestatus?: Tiltakstypestatus;
}

export function useAlleTiltakstyper(
  filter: Filter = { tiltakstypestatus: undefined }
) {
  return useQuery(QueryKeys.alleTiltakstyper(), () =>
    mulighetsrommetClient.tiltakstyper.getTiltakstyper({
      search: undefined,
      tiltakstypestatus: filter.tiltakstypestatus,
      tiltakstypekategori: undefined,
      sort: undefined,
      page: 1,
      size: 1000,
    })
  );
}
