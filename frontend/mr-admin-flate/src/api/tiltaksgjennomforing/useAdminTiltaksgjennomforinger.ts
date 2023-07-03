import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "../QueryKeys";
import { paginationAtom, tiltaksgjennomforingfilter } from "../atoms";
import { mulighetsrommetClient } from "../clients";

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
        fylkesenhet: filter.navRegion ? filter.navRegion : undefined,
        sort: filter.sortering ? filter.sortering : undefined,
        size: filter.antallGjennomforingerVises,
        avtaleId: filter.avtale ? filter.avtale : undefined,
        arrangorOrgnr: filter.arrangorOrgnr ? filter.arrangorOrgnr : undefined,
      })
  );
}
