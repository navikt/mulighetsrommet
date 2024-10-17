import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltaksgjennomforingFilter } from "../atoms";
import { type GetTiltaksgjennomforingerData, TiltaksgjennomforingerService } from "@mr/api-client";
import { getPublisertStatus } from "../../utils/Utils";

export function useAdminTiltaksgjennomforinger(filter: Partial<TiltaksgjennomforingFilter>) {
  const debouncedSok = useDebounce(filter.search?.trim(), 300);

  const queryFilter: GetTiltaksgjennomforingerData = {
    search: debouncedSok || undefined,
    navEnheter: filter.navEnheter?.map((e) => e.enhetsnummer) ?? [],
    tiltakstyper: filter.tiltakstyper,
    statuser: filter.statuser,
    sort: filter.sortering ? filter.sortering.sortString : undefined,
    page: filter.page ?? 1,
    size: filter.pageSize,
    avtaleId: filter.avtale ? filter.avtale : undefined,
    publisert: getPublisertStatus(filter.publisert),
    arrangorer: filter.arrangorer,
  };

  return useQuery({
    queryKey: QueryKeys.tiltaksgjennomforinger(filter.visMineGjennomforinger, queryFilter),
    queryFn: () =>
      filter.visMineGjennomforinger
        ? TiltaksgjennomforingerService.getMineTiltaksgjennomforinger(queryFilter)
        : TiltaksgjennomforingerService.getTiltaksgjennomforinger(queryFilter),
  });
}
