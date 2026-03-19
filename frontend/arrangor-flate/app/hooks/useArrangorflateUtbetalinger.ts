import { useSuspenseQuery } from "@tanstack/react-query";
import {
  ArrangorflateService,
  ArrangorflateUtbetalingFilterDirection,
  ArrangorflateUtbetalingFilterOrderBy,
  ArrangorflateUtbetalingFilterType,
  GetArrangorflateUtbetalingerData,
} from "api-client";
import { useState } from "react";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

const PAGE_SIZE = 10;

export type ArrangorflateUtbetalingFilter = GetArrangorflateUtbetalingerData["query"];

const defaultFilter = {
  page: 1,
  size: PAGE_SIZE,
  type: ArrangorflateUtbetalingFilterType.AKTIVE,
  orderBy: ArrangorflateUtbetalingFilterOrderBy.ARRANGOR,
  direction: ArrangorflateUtbetalingFilterDirection.ASC,
};

export function useArrangorflateUtbetalinger(
  initialFilter: Partial<ArrangorflateUtbetalingFilter>,
) {
  const [filter, setFilter] = useState<ArrangorflateUtbetalingFilter>({
    ...defaultFilter,
    ...initialFilter,
  });

  return {
    ...useSuspenseQuery({
      queryKey: queryKeys.utbetalinger(filter),
      queryFn: async () => {
        const result = await ArrangorflateService.getArrangorflateUtbetalinger({
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
