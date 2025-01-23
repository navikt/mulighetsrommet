import { QueryKeys } from "@/api/QueryKeys";
import { useApiQuery } from "@mr/frontend-common";
import { useDebounce } from "@mr/frontend-common";
import { AvtaleFilter } from "../atoms";
import { AvtalerService, type GetAvtalerData } from "@mr/api-client-v2";

export function useAvtaler(filter: Partial<AvtaleFilter>) {
  const debouncedSok = useDebounce(filter.sok?.trim(), 300);

  const queryFilter: GetAvtalerData = {
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
