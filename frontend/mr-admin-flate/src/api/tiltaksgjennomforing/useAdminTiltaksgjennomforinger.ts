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

  const queryFilter = {
    page,
    search: debouncedSok || undefined,
    navEnhet: filter.navEnhet ? filter.navEnhet : undefined,
    tiltakstypeId: filter.tiltakstype ? filter.tiltakstype : undefined,
    status: filter.status ? filter.status : undefined,
    navRegion: filter.navRegion ? filter.navRegion : undefined,
    sort: filter.sortering ? filter.sortering : undefined,
    size: filter.antallGjennomforingerVises,
    avtaleId: filter.avtale ? filter.avtale : undefined,
    arrangorOrgnr: filter.arrangorOrgnr ? filter.arrangorOrgnr : undefined,
  };

  return useQuery(
    QueryKeys.tiltaksgjennomforinger({ ...filter, search: debouncedSok }, page),
    () =>
      filter.visMineGjennomforinger
        ? mulighetsrommetClient.tiltaksgjennomforinger.getMineTiltaksgjennomforinger({
            ...queryFilter,
          })
        : mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
            ...queryFilter,
          }),
  );
}
