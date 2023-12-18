import { useQuery } from "@tanstack/react-query";
import { WritableAtom, useAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "../QueryKeys";
import { TiltaksgjennomforingfilterProps, gjennomforingPaginationAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";

export function useAdminTiltaksgjennomforinger(
  filterAtom: WritableAtom<
    TiltaksgjennomforingfilterProps,
    [newValue: TiltaksgjennomforingfilterProps],
    void
  >,
) {
  const [page] = useAtom(gjennomforingPaginationAtom);
  const [filter] = useAtom(filterAtom);
  const debouncedSok = useDebounce(filter.search, 300);

  const queryFilter = {
    page,
    search: debouncedSok || undefined,
    navEnheter: filter.navEnheter,
    tiltakstypeIder: filter.tiltakstyper,
    statuser: filter.statuser,
    navRegioner: filter.navRegioner,
    sort: filter.sortering ? filter.sortering : undefined,
    size: filter.antallGjennomforingerVises,
    avtaleId: filter.avtale ? filter.avtale : undefined,
    arrangorOrgnr: filter.arrangorOrgnr,
  };

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforinger(page, { ...filter, search: debouncedSok }),
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
