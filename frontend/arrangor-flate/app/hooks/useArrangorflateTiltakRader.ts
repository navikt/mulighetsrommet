import { useSuspenseQuery } from "@tanstack/react-query";
import {
  ArrangorflateFilterDirection,
  ArrangorflateFilterType,
  ArrangorflateService,
  ArrangorflateTiltakFilterOrderBy,
  GetArrangorTiltaksoversiktData,
} from "api-client";
import { useCallback, useState } from "react";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

const PAGE_SIZE = 25;

export type ArrangorflateTiltakFilter = NonNullable<GetArrangorTiltaksoversiktData["query"]>;

function defaultFilter(type?: ArrangorflateTiltakFilter["type"]): ArrangorflateTiltakFilter {
  const defaultType = type || ArrangorflateFilterType.AKTIVE;
  const base = {
    page: 1,
    size: PAGE_SIZE,
    type: defaultType,
    orderBy: ArrangorflateTiltakFilterOrderBy.START_DATO,
    direction: ArrangorflateFilterDirection.DESC,
  };
  if (type === ArrangorflateFilterType.AKTIVE) {
    return {
      ...base,
      orderBy: ArrangorflateTiltakFilterOrderBy.TILTAK,
      direction: ArrangorflateFilterDirection.ASC,
      size: undefined, // Vis alle
    };
  }
  return base;
}

export function useArrangorTiltaksoversikt(initialFilter: Partial<ArrangorflateTiltakFilter>) {
  const [filter, setFilter] = useState<ArrangorflateTiltakFilter>({
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
    filter,
    setFilter,
    oppdaterSok,
    ...useSuspenseQuery({
      queryKey: queryKeys.tiltaksoversikt(filter),
      queryFn: async () => {
        const result = await ArrangorflateService.getArrangorTiltaksoversikt({
          query: { ...filter },
          client: queryClient,
        });
        if (result.error) {
          throw result.error;
        }
        return result.data;
      },
    }),
  };
}
