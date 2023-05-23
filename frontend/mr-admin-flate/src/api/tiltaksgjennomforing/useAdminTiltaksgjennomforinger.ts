import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom, tiltaksgjennomforingfilter } from "../atoms";
import { useDebounce } from "mulighetsrommet-frontend-common";

export function useAdminTiltaksgjennomforinger() {
  const [page] = useAtom(paginationAtom);
  const [filter] = useAtom(tiltaksgjennomforingfilter);
  const debouncedSok = useDebounce(filter.search, 300);
  return useQuery(
    QueryKeys.tiltaksgjennomforinger({ ...filter, search: debouncedSok }, page),
    () =>
      mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
        page,
        search: debouncedSok || undefined,
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
