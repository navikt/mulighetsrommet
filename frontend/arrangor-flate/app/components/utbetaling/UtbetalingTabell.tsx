import {
  ArrangorflateUtbetalingFilterDirection,
  ArrangorflateUtbetalingFilterOrderBy,
  ArrangorflateUtbetalingFilterType,
} from "@api-client";
import { Suspense, useEffect, useState } from "react";
import {
  paramToSortDirection,
  paramToSortKey,
  sortKeyToParam,
  utbetalingKolonner,
  UtbetalingRow,
} from "../common/UtbetalingRow";
import { Box, PaginationProps, Search, SortState } from "@navikt/ds-react";
import {
  ArrangorflateUtbetalingFilter,
  useArrangorflateUtbetalinger,
} from "~/hooks/useArrangorflateUtbetalinger";
import { useDebounce } from "@mr/frontend-common";
import { Tabellvisning } from "../common/Tabellvisning";
import { Laster } from "../common/Laster";

function filterToSortState({ orderBy, direction }: ArrangorflateUtbetalingFilter): SortState {
  const newOrderBy: SortState["orderBy"] = (orderBy && paramToSortKey[orderBy]) || "tiltaksNavn";
  const newDirection: SortState["direction"] =
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    (direction && paramToSortDirection[direction]) || "ascending";

  return {
    orderBy: newOrderBy,
    direction: newDirection,
  };
}

interface UtbetalingTabellProps {
  type: ArrangorflateUtbetalingFilterType;
}
export function UtbetalingTabell({ type }: UtbetalingTabellProps) {
  const [sok, setSok] = useState("");
  const debouncedSok = useDebounce(sok, 300);
  const {
    data: paginertUtbetalingRader,
    filter,
    setFilter,
    oppdaterSok,
  } = useArrangorflateUtbetalinger({ type });

  const [sortState, setSortState] = useState<SortState>(filterToSortState(filter));

  useEffect(() => {
    oppdaterSok(debouncedSok);
  }, [debouncedSok, oppdaterSok]);

  function clearSearch() {
    setSok("");
  }

  useEffect(() => {
    const filterSortState = filterToSortState(filter);
    setSortState(filterSortState);
  }, [filter, setSortState]);

  const paginationProps: PaginationProps | undefined =
    type === ArrangorflateUtbetalingFilterType.HISTORISKE
      ? {
          hidden: !paginertUtbetalingRader.pagination.totalPages,
          page: filter.page || 1,
          count: paginertUtbetalingRader.pagination.totalPages || 1,
          boundaryCount: 1,
          prevNextTexts: true,
          onPageChange: (newPage) => setFilter((filter) => ({ ...filter, page: newPage })),
        }
      : undefined;

  function sortChange(orderBy: ArrangorflateUtbetalingFilterOrderBy) {
    if (orderBy == filter.orderBy) {
      const direction =
        filter.direction == ArrangorflateUtbetalingFilterDirection.ASC
          ? ArrangorflateUtbetalingFilterDirection.DESC
          : ArrangorflateUtbetalingFilterDirection.ASC;
      return setFilter((old) => ({ ...old, direction }));
    }

    setFilter((old) => ({
      ...old,
      orderBy,
      direction: ArrangorflateUtbetalingFilterDirection.ASC,
    }));
  }

  return (
    <>
      <Box paddingBlock="space-16" width="30rem">
        <Search
          label="Søk i utbetalinger"
          description="Tiltaksnavn, arrangør, periode, beløp"
          hideLabel={false}
          variant="simple"
          width="30rem"
          onChange={setSok}
          onClear={clearSearch}
        />
      </Box>
      <Tabellvisning
        kolonner={utbetalingKolonner}
        sort={sortState}
        onSortChange={(key) => sortChange(sortKeyToParam[key])}
        pagination={paginationProps}
      >
        <Suspense fallback={<Laster tekst="Laster data..." size="xlarge" />}>
          {paginertUtbetalingRader.data.map((rad, i) => (
            <UtbetalingRow key={rad.gjennomforingId + i} row={rad} />
          ))}
        </Suspense>
      </Tabellvisning>
    </>
  );
}
