import { QueryKeys } from "@/api/QueryKeys";
import { useApiQuery, useDebounce } from "@mr/frontend-common";
import { AvtalerService, type GetAvtalerData } from "@mr/api-client-v2";
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
    },
  };

  return useApiQuery({
    queryKey: QueryKeys.avtaler(filter.visMineAvtaler, queryFilter),

    queryFn: () => {
      return filter.visMineAvtaler
        ? AvtalerService.getMineAvtaler(queryFilter)
        : AvtalerService.getAvtaler(queryFilter);
    },
  });
}
