import { useApiQuery, useDebounce } from "@mr/frontend-common";
import { ArrangorService, ArrangorTil, type GetArrangorerData } from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorerFilterType } from "@/pages/arrangor/filter";

export function useArrangorer(til?: ArrangorTil, filter?: Partial<ArrangorerFilterType>) {
  const debouncedSok = useDebounce(filter?.sok?.trim(), 300);

  const arrangorFilter: Pick<GetArrangorerData, "query"> = {
    query: {
      til,

      sok: debouncedSok || undefined,
      page: filter?.page,
      size: filter?.pageSize,
      sortering: filter?.sortering?.sortString,
    },
  };

  return useApiQuery({
    queryKey: QueryKeys.arrangorer(arrangorFilter),

    queryFn: () => {
      return ArrangorService.getArrangorer(arrangorFilter);
    },
  });
}
