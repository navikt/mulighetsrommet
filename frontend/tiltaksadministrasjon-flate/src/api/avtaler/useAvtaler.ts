import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery, useDebounce } from "@mr/frontend-common";
import { AvtaleService, type GetAvtalerData } from "@tiltaksadministrasjon/api-client";
import { AvtaleFilterType } from "@/pages/avtaler/filter";

export function useAvtaler(filter: Partial<AvtaleFilterType>) {
  const debouncedSok = useDebounce(filter.sok?.trim(), 300);

  const request: Pick<GetAvtalerData, "body" | "query"> = {
    body: {
      tiltakstyper: filter.tiltakstyper ?? [],
      search: debouncedSok || null,
      statuser: filter.statuser ?? [],
      avtaletyper: filter.avtaletyper ?? [],
      navEnheter: filter.navEnheter ?? [],
      arrangorer: filter.arrangorer ?? [],
      personvernBekreftet: filter.personvernBekreftet ?? null,
      visMineAvtaler: filter.visMineAvtaler ?? false,
      sort: filter.sortering?.sortString ?? null,
    },
    query: {
      page: filter.page ?? 1,
      size: filter.pageSize,
    },
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.avtaler(request),
    queryFn: () => AvtaleService.getAvtaler(request),
  });
}

export function useAvtalerHandlinger() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.avtaleHandlinger(),
    queryFn: async () => {
      return AvtaleService.getAvtaleHandlingerForNyeAvtaler();
    },
  });
}
