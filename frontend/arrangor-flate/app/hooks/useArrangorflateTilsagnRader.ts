import { useSuspenseQuery } from "@tanstack/react-query";
import {
  ArrangorflateService,
  ArrangorflateFilterDirection,
  ArrangorflateTilsagnFilterOrderBy,
  GetArrangorflateTilsagnRaderData,
} from "api-client";
import { useState } from "react";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

const PAGE_SIZE = 25;

export type ArrangorflateTilsagnFilter = NonNullable<GetArrangorflateTilsagnRaderData["query"]>;

function defaultFilter(): ArrangorflateTilsagnFilter {
  return {
    page: 1,
    size: PAGE_SIZE,
    orderBy: ArrangorflateTilsagnFilterOrderBy.PERIODE,
    direction: ArrangorflateFilterDirection.DESC,
  };
}

export function useArrangorflateTilsagnRader() {
  const [filter, setFilter] = useState<ArrangorflateTilsagnFilter>({
    ...defaultFilter(),
  });

  return {
    ...useSuspenseQuery({
      queryKey: queryKeys.tilsagnRader(filter),
      queryFn: async () => {
        const result = await ArrangorflateService.getArrangorflateTilsagnRader({
          query: filter,
          client: queryClient,
        });
        if (result.error) {
          throw result.error;
        }
        return result.data;
      },
    }),
    filter,
    setFilter,
  };
}
