import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery, useDebounce } from "@mr/frontend-common";
import { AvtaleService, type GetAvtalerData } from "@tiltaksadministrasjon/api-client";
import { AvtaleFilterType } from "@/pages/avtaler/filter";

export function useAvtaler(filter: Partial<AvtaleFilterType>) {
  const debouncedSok = useDebounce(filter.sok?.trim(), 300);

  const queryFilter: Pick<GetAvtalerData, "query"> = {
    query: {
      tiltakstyper: filter.tiltakstyper,
      search: debouncedSok || undefined,
      statuser: filter.statuser,
      avtaletyper: filter.avtaletyper,
      navRegioner: filter.navRegioner,
      sort: filter.sortering?.sortString,
      page: filter.page ?? 1,
      size: filter.pageSize,
      arrangorer: filter.arrangorer,
      personvernBekreftet: filter.personvernBekreftet,
      visMineAvtaler: filter.visMineAvtaler,
    },
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.avtaler(queryFilter),
    queryFn: () => AvtaleService.getAvtaler(queryFilter),
  });
}
