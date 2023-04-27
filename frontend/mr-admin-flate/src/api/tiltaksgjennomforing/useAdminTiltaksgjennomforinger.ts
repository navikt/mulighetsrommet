import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom, tiltaksgjennomforingfilter } from "../atoms";

export function useAdminTiltaksgjennomforinger() {
  const [page] = useAtom(paginationAtom);
  const [filter] = useAtom(tiltaksgjennomforingfilter);
  return useQuery(QueryKeys.tiltaksgjennomforinger(filter, page), () =>
    mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
      page,
      search: filter.search ? filter.search : undefined,
      enhet: filter.enhet ? filter.enhet : undefined,
      tiltakstypeId: filter.tiltakstype ? filter.tiltakstype : undefined,
      status: filter.status ? filter.status : undefined,
      fylkesenhet: filter.fylkesenhet ? filter.fylkesenhet : undefined,
      sort: filter.sortering ? filter.sortering : undefined,
      size: PAGE_SIZE,
      avtaleId: filter.avtale ? filter.avtale : undefined,
    })
  );
}
