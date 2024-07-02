import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingFilter } from "../atoms";
import { TiltaksgjennomforingerService } from "mulighetsrommet-api-client";

function getPublisertStatus(statuser: string[] = []): boolean | null {
  if (statuser.length === 0) return null;

  if (statuser.every((status) => status === "publisert")) return true;

  if (statuser.every((status) => status === "ikke-publisert")) return false;

  return null;
}

export function useAdminTiltaksgjennomforinger(filter: Partial<TiltaksgjennomforingFilter>) {
  const debouncedSok = useDebounce(filter.search?.trim(), 300);

  const queryFilter = {
    search: debouncedSok || undefined,
    navEnheter: filter.navEnheter?.map((e) => e.enhetsnummer) ?? [],
    tiltakstyper: filter.tiltakstyper,
    statuser: filter.statuser,
    sort: filter.sortering ? filter.sortering : undefined,
    page: filter.page ?? 1,
    size: filter.pageSize,
    avtaleId: filter.avtale ? filter.avtale : undefined,
    publisert: getPublisertStatus(filter.publisert),
    arrangorer: filter.arrangorer,
  };

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforinger(filter.visMineGjennomforinger, queryFilter.page, {
      ...filter,
      search: debouncedSok,
    }),
    queryFn: () =>
      filter.visMineGjennomforinger
        ? TiltaksgjennomforingerService.getMineTiltaksgjennomforinger(queryFilter)
        : TiltaksgjennomforingerService.getTiltaksgjennomforinger(queryFilter),
  });
}
