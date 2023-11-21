import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "../QueryKeys";
import { paginationAtom, tiltaksgjennomforingfilterAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";

export function useAdminTiltaksgjennomforinger() {
  const [page] = useAtom(paginationAtom);
  const [filter] = useAtom(tiltaksgjennomforingfilterAtom);
  const debouncedSok = useDebounce(filter.search, 300);

  const queryFilter = {
    page,
    search: debouncedSok || undefined,
    navEnhet: filter.navEnhet,
    tiltakstypeId: filter.tiltakstype || undefined,
    status: filter.status,
    navRegion: filter.navRegion,
    sort: filter.sortering,
    size: filter.antallGjennomforingerVises,
    avtaleId: filter.avtale,
    arrangorOrgnr: filter.arrangorOrgnr,
  };

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforinger({ ...filter, search: debouncedSok }, page),

    queryFn: () =>
      filter.visMineGjennomforinger
        ? mulighetsrommetClient.tiltaksgjennomforinger.getMineTiltaksgjennomforinger({
            ...queryFilter,
          })
        : mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger({
            ...queryFilter,
          }),
  });
}
