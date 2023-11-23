import { useQuery } from "@tanstack/react-query";
import { WritableAtom, useAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "../QueryKeys";
import { TiltaksgjennomforingfilterProps, paginationAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";

export function useAdminTiltaksgjennomforinger(
  filterAtom: WritableAtom<
    TiltaksgjennomforingfilterProps,
    [newValue: TiltaksgjennomforingfilterProps],
    void
  >,
) {
  const [page] = useAtom(paginationAtom);
  const [filter] = useAtom(filterAtom);
  const debouncedSok = useDebounce(filter.search, 300);

  const queryFilter = {
    page,
    search: debouncedSok || undefined,
    navEnheter: filter.navEnhet ? [filter.navEnhet] : [],
    tiltakstypeIder: filter.tiltakstype ? [filter.tiltakstype] : [],
    statuser: filter.status ? [filter.status] : [],
    navRegioner: filter.navRegion ? [filter.navRegion] : [],
    sort: filter.sortering ? filter.sortering : undefined,
    size: filter.antallGjennomforingerVises,
    avtaleId: filter.avtale ? filter.avtale : undefined,
    arrangorOrgnr: filter.arrangorOrgnr ? [filter.arrangorOrgnr] : [],
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
