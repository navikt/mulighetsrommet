import { useQuery } from "@tanstack/react-query";
import { ArrangorTil } from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorerFilter } from "../atoms";
import { useDebounce } from "mulighetsrommet-frontend-common";

export function useArrangorer(til?: ArrangorTil, filter?: Partial<ArrangorerFilter>) {
  const debouncedSok = useDebounce(filter?.sok?.trim(), 300);

  return useQuery({
    queryKey: QueryKeys.arrangorer(til, filter?.page, { ...filter, sok: debouncedSok }),

    queryFn: () => {
      return mulighetsrommetClient.arrangor.getArrangorer({
        til,
        sok: debouncedSok || undefined,
        page: filter?.page,
        size: filter?.pageSize,
        sortering: filter?.sortering,
      });
    },
  });
}
