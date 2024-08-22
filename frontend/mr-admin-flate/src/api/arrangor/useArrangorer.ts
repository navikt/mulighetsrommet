import { useQuery } from "@tanstack/react-query";
import { ArrangorService, ArrangorTil, type GetArrangorerData } from "@mr/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorerFilter } from "../atoms";
import { useDebounce } from "@mr/frontend-common";

export function useArrangorer(til?: ArrangorTil, filter?: Partial<ArrangorerFilter>) {
  const debouncedSok = useDebounce(filter?.sok?.trim(), 300);

  const arrangorFilter: GetArrangorerData = {
    til,
    sok: debouncedSok || undefined,
    page: filter?.page,
    size: filter?.pageSize,
    sortering: filter?.sortering?.sortString,
  };

  return useQuery({
    queryKey: QueryKeys.arrangorer(arrangorFilter),

    queryFn: () => {
      return ArrangorService.getArrangorer(arrangorFilter);
    },
  });
}
