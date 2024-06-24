import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";
import { useQuery } from "@tanstack/react-query";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { AvtaleFilter } from "../atoms";

export function useAvtaler(filter: Partial<AvtaleFilter>) {
  const debouncedSok = useDebounce(filter.sok?.trim(), 300);

  const queryFilter = {
    tiltakstyper: filter.tiltakstyper,
    search: debouncedSok || undefined,
    statuser: filter.statuser,
    avtaletyper: filter.avtaletyper,
    navRegioner: filter.navRegioner,
    sort: filter.sortering,
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
        ? mulighetsrommetClient.avtaler.getMineAvtaler({ ...queryFilter })
        : mulighetsrommetClient.avtaler.getAvtaler({ ...queryFilter });
    },
  });
}
