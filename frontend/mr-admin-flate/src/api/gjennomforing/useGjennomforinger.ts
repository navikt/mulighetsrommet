import { useApiSuspenseQuery, useDebounce } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import {
  type GetGjennomforingerData,
  GjennomforingService,
} from "@tiltaksadministrasjon/api-client";
import { getPublisertStatus } from "@/utils/Utils";
import { GjennomforingFilterType } from "@/pages/gjennomforing/filter";

export function useGjennomforinger(filter: Partial<GjennomforingFilterType>) {
  const debouncedSok = useDebounce(filter.search?.trim(), 300);

  const request: Pick<GetGjennomforingerData, "body" | "query"> = {
    body: {
      search: debouncedSok || null,
      navEnheter: filter.navEnheter ?? [],
      tiltakstyper: filter.tiltakstyper ?? [],
      statuser: filter.statuser ?? [],
      gjennomforingTyper: filter.gjennomforingTyper ?? [],
      arrangorer: filter.arrangorer ?? [],
      sort: filter.sortering ? filter.sortering.sortString : null,
      avtaleId: filter.avtale ? filter.avtale : null,
      publisert: getPublisertStatus(filter.publisert) ?? null,
      visMineGjennomforinger: filter.visMineGjennomforinger ?? false,
    },
    query: {
      page: filter.page ?? 1,
      size: filter.pageSize,
    },
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.gjennomforinger(request),
    queryFn: () => GjennomforingService.getGjennomforinger(request),
  });
}
