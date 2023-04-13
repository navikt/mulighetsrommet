import { useQuery } from "@tanstack/react-query";
import {
  Tiltakstypekategori,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

interface Filter {
  tiltakstypestatus?: Tiltakstypestatus;
  tiltakstypekategori?: Tiltakstypekategori;
}

export function useAlleTiltakstyper(
  filter: Filter = {
    tiltakstypestatus: undefined,
    tiltakstypekategori: undefined,
  }
) {
  return useQuery(QueryKeys.alleTiltakstyper(), () =>
    mulighetsrommetClient.tiltakstyper.getTiltakstyper({
      search: undefined,
      tiltakstypestatus: filter.tiltakstypestatus,
      tiltakstypekategori: filter.tiltakstypekategori,
      sort: undefined,
      page: 1,
      size: 1000,
    })
  );
}
