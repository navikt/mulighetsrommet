import { useApiQuery, useDebounce } from "@mr/frontend-common";
import {
  ArrangorKobling,
  ArrangorService,
  GetArrangorerData,
} from "@tiltaksadministrasjon/api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorerFilterType } from "@/pages/arrangor/filter";

export function useArrangorer(kobling?: ArrangorKobling, filter?: Partial<ArrangorerFilterType>) {
  const debouncedSok = useDebounce(filter?.sok?.trim(), 300);

  const arrangorFilter: Pick<GetArrangorerData, "query"> = {
    query: {
      kobling,
      sok: debouncedSok || undefined,
      page: filter?.page,
      size: filter?.pageSize,
      sort: filter?.sortering?.sortString,
    },
  };

  return useApiQuery({
    queryKey: QueryKeys.arrangorer(arrangorFilter),

    queryFn: () => {
      return ArrangorService.getArrangorer(arrangorFilter);
    },
  });
}
