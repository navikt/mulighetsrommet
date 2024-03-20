import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "../QueryKeys";
import { TiltaksgjennomforingFilter } from "../atoms";
import { mulighetsrommetClient } from "../clients";

export function useAdminTiltaksgjennomforinger(filter: Partial<TiltaksgjennomforingFilter>) {
  const debouncedSok = useDebounce(filter.search?.trim(), 300);

  const queryFilter = {
    search: debouncedSok || undefined,
    navEnheter: filter.navEnheter?.map((e) => e.enhetsnummer) ?? [],
    tiltakstypeIder: filter.tiltakstyper,
    statuser: filter.statuser,
    sort: filter.sortering ? filter.sortering : undefined,
    page: filter.page ?? 1,
    size: filter.pageSize,
    avtaleId: filter.avtale ? filter.avtale : undefined,
    arrangorOrgnr: filter.arrangorOrgnr,
  };

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforinger(filter.visMineGjennomforinger, queryFilter.page, {
      ...filter,
      search: debouncedSok,
    }),
    queryFn: () =>
      filter.visMineGjennomforinger
        ? mulighetsrommetClient.tiltaksgjennomforinger.getMineTiltaksgjennomforinger(queryFilter)
        : mulighetsrommetClient.tiltaksgjennomforinger.getTiltaksgjennomforinger(queryFilter),
  });
}
