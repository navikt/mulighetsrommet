import { useSuspenseQuery } from "@tanstack/react-query";
import {
  ArrangorflateService,
  ArrangorflateFilterDirection,
  ArrangorflateUtbetalingFilterOrderBy,
  ArrangorflateFilterType,
  GetArrangorflateUtbetalingerData,
} from "api-client";
import { useCallback, useState } from "react";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

const PAGE_SIZE = 25;

export type ArrangorflateUtbetalingFilter = NonNullable<GetArrangorflateUtbetalingerData["query"]>;

function defaultFilter(type?: ArrangorflateFilterType): ArrangorflateUtbetalingFilter {
  const defaultType = type || ArrangorflateFilterType.AKTIVE;
  const base = {
    page: 1,
    size: PAGE_SIZE,
    type: defaultType,
    orderBy: ArrangorflateUtbetalingFilterOrderBy.PERIODE,
    direction: ArrangorflateFilterDirection.DESC,
  };
  if (type === ArrangorflateFilterType.AKTIVE) {
    return {
      ...base,
      orderBy: ArrangorflateUtbetalingFilterOrderBy.TILTAK,
      direction: ArrangorflateFilterDirection.ASC,
      size: undefined, // Vis alle
    };
  }
  return base;
}

export function useArrangorflateUtbetalingRader(
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
