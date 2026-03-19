import { useSuspenseQuery } from "@tanstack/react-query";
import {
  ArrangorflateService,
  ArrangorflateUtbetalingFilterDirection,
  ArrangorflateUtbetalingFilterOrderBy,
  ArrangorflateUtbetalingFilterType,
  GetArrangorflateUtbetalingerData,
} from "api-client";
import { useCallback, useState } from "react";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

const PAGE_SIZE = 25;

export type ArrangorflateUtbetalingFilter = NonNullable<GetArrangorflateUtbetalingerData["query"]>;

function defaultFilter(type?: ArrangorflateUtbetalingFilterType): ArrangorflateUtbetalingFilter {
  const defaultType = type || ArrangorflateUtbetalingFilterType.AKTIVE;
  const base = {
    page: 1,
    size: PAGE_SIZE,
    type: defaultType,
    orderBy: ArrangorflateUtbetalingFilterOrderBy.PERIODE,
    direction: ArrangorflateUtbetalingFilterDirection.DESC,
  };
  if (type === ArrangorflateUtbetalingFilterType.AKTIVE) {
    return {
      ...base,
      orderBy: ArrangorflateUtbetalingFilterOrderBy.TILTAK,
      size: undefined,
    };
  }
  return base;
}

export function useArrangorflateUtbetalinger(
  initialFilter: Partial<ArrangorflateUtbetalingFilter>,
) {
  const [filter, setFilter] = useState<ArrangorflateUtbetalingFilter>({
    ...defaultFilter(initialFilter.type),
    ...initialFilter,
  });

  const oppdaterSok = useCallback(
    (val: string) => {
      const trimmedValue = val.trim();
      if (filter.sok !== trimmedValue)
        setFilter((old) => ({ ...old, sok: trimmedValue || undefined }));
    },
    [filter.sok, setFilter],
  );

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
    oppdaterSok,
  };
}
