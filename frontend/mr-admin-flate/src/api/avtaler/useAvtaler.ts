import { QueryKeys } from "@/api/QueryKeys";
import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "@mr/frontend-common";
import { AvtaleFilter } from "../atoms";
import { AvtalerService } from "@mr/api-client";

export function useAvtaler(filter: Partial<AvtaleFilter>) {
  const debouncedSok = useDebounce(filter.sok?.trim(), 300);

  const queryFilter = {
    tiltakstyper: filter.tiltakstyper,
    search: debouncedSok || undefined,
    statuser: filter.statuser,
    avtaletyper: filter.avtaletyper,
    navRegioner: filter.navRegioner,
    sort: filter.sortering?.sortString,
    page: filter.page ?? 1,
    size: filter.pageSize,
    arrangorer: filter.arrangorer,
    personvernBekreftet:
      filter.personvernBekreftet?.length === 1 ? filter.personvernBekreftet[0] : undefined,
  };

  return useQuery({
    queryKey: QueryKeys.avtaler(filter.visMineAvtaler, queryFilter.page, {
      ...filter,
      sok: debouncedSok,
    }),

    queryFn: () => {
      return filter.visMineAvtaler
        ? AvtalerService.getMineAvtaler({ ...queryFilter })
        : AvtalerService.getAvtaler({ ...queryFilter });
    },
  });
}
