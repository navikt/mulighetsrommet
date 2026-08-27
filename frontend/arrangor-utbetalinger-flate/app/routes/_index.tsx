import { Link as ReactRouterLink } from "react-router";
import { Box, Tabs, Button, HStack, PaginationProps, Search, SortState } from "@navikt/ds-react";
import {
  ArrangorflateTilsagnFilterOrderBy,
  ArrangorflateFilterDirection,
  ArrangorflateUtbetalingFilterOrderBy,
  ArrangorflateFilterType,
} from "@arrangor-utbetalinger/api-client";
import type { MetaFunction } from "react-router";
import { PageHeading } from "~/components/common/PageHeading";
import { useTabState } from "~/hooks/useTabState";
import { tekster } from "~/tekster";
import { useDebounce } from "@mr/frontend-common";
import { pathTo } from "~/utils/navigation";
import { Tabellvisning } from "~/components/common/Tabellvisning";
import { utbetalingKolonner, UtbetalingRow } from "~/components/common/UtbetalingRow";
import { tilsagnKolonner, TilsagnRow } from "~/components/common/TilsagnRow";
import { Suspense, useEffect, useState } from "react";
import { Laster } from "~/components/common/Laster";
import {
  ArrangorflateTilsagnFilter,
  useArrangorflateTilsagnRader,
} from "~/hooks/useArrangorflateTilsagnRader";
import { useArrangorflateUtbetalingRader } from "~/hooks/useArrangorflateUtbetalingRader";
import { flipObject } from "~/utils/object";
import { IngenTreff } from "~/components/IngenTreff";

export const meta: MetaFunction = () => {
  return [
    { title: "Utbetalinger til tiltaksarrangør" },
    { name: "description", content: "Arrangørflate for krav om utbetalinger" },
  ];
};

export default function Oversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");

  return (
    <Box>
      <HStack justify="space-between" align="center">
        <PageHeading title={tekster.bokmal.utbetaling.headingTitle} />
        <Button variant="secondary" as={ReactRouterLink} to={pathTo.tiltaksoversikt}>
          {tekster.bokmal.utbetaling.opprettUtbetaling.actionLabel}
        </Button>
      </HStack>
      <Tabs
        value={currentTab}
        onChange={(tab) => setTab(tab as "aktive" | "historiske" | "tilsagnsoversikt")}
      >
        <Tabs.List>
          <Tabs.Tab value="aktive" label={tekster.bokmal.utbetaling.oversiktFaner.aktive} />
          <Tabs.Tab value="historiske" label={tekster.bokmal.utbetaling.oversiktFaner.historiske} />
          <Tabs.Tab
            value="tilsagnsoversikt"
            label={tekster.bokmal.utbetaling.oversiktFaner.tilsagnsoversikt}
          />
        </Tabs.List>
        <Tabs.Panel value={currentTab}>
          {currentTab === "tilsagnsoversikt" ? (
            <TilsagnTabellContent />
          ) : (
            <UtbetalingTabellContent
              key={currentTab}
              type={
                currentTab === "aktive"
                  ? ArrangorflateFilterType.AKTIVE
                  : ArrangorflateFilterType.HISTORISKE
              }
            />
          )}
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}

function UtbetalingTabellContent({ type }: { type: ArrangorflateFilterType }) {
  const {
    data: paginertUtbetalingRader,
    filter,
    setFilter,
    oppdaterSok,
  } = useArrangorflateUtbetalingRader({ type });
  const [, setSok, clearSearch] = useTableSearch(oppdaterSok);
  const sortKeyToParam: Record<string, ArrangorflateUtbetalingFilterOrderBy> = {
    tiltakNavn: ArrangorflateUtbetalingFilterOrderBy.TILTAK,
    arrangorNavn: ArrangorflateUtbetalingFilterOrderBy.ARRANGOR,
    startDato: ArrangorflateUtbetalingFilterOrderBy.PERIODE,
    beregnetBelop: ArrangorflateUtbetalingFilterOrderBy.BEREGNET_BELOP,
    godkjentBelop: ArrangorflateUtbetalingFilterOrderBy.GODKJENT_BELOP,
    status: ArrangorflateUtbetalingFilterOrderBy.STATUS,
  };

  const paginationProps: PaginationProps | undefined =
    type === ArrangorflateFilterType.HISTORISKE
      ? {
          hidden: !paginertUtbetalingRader.pagination.totalPages,
          page: filter.page || 1,
          count: paginertUtbetalingRader.pagination.totalPages || 1,
          boundaryCount: 1,
          prevNextTexts: true,
          onPageChange: (newPage) => setFilter((filter) => ({ ...filter, page: newPage })),
        }
      : undefined;

  return (
    <>
      <Box paddingBlock="space-16" width="30rem">
        <Search
          label="Søk i utbetalinger"
          description="Tiltaksnavn, arrangør, periode, beløp"
          hideLabel={false}
          variant="simple"
          onChange={setSok}
          onClear={clearSearch}
        />
      </Box>
      <Tabellvisning
        kolonner={utbetalingKolonner(type)}
        sort={filterToSortState(filter, sortKeyToParam, "tiltakNavn")}
        onSortChange={(key) =>
          setFilter((old) => ({ ...old, ...nextSort(filter, sortKeyToParam[key]) }))
        }
        pagination={paginationProps}
      >
        <Suspense fallback={<Laster tekst="Laster data..." size="xlarge" />}>
          {paginertUtbetalingRader.data.map((rad, i) => (
            <UtbetalingRow key={rad.gjennomforing.id + i} row={rad} type={type} />
          ))}
        </Suspense>
      </Tabellvisning>
      {paginertUtbetalingRader.data.length === 0 && <IngenTreff type="utbetaling" />}
    </>
  );
}

function TilsagnTabellContent() {
  const {
    data: paginertTilsagnRader,
    filter,
    setFilter,
    updateSearch,
  } = useArrangorflateTilsagnRader();
  const [, setSearch, clearSearch] = useTableSearch(updateSearch);

  const paginationProps: PaginationProps = {
    hidden: !paginertTilsagnRader.pagination.totalPages,
    page: filter.page || 1,
    count: paginertTilsagnRader.pagination.totalPages || 1,
    boundaryCount: 1,
    prevNextTexts: true,
    onPageChange: (newPage) =>
      setFilter((filter: ArrangorflateTilsagnFilter) => ({ ...filter, page: newPage })),
  };

  const sortKeyToParam: Record<string, ArrangorflateTilsagnFilterOrderBy> = {
    tiltakNavn: ArrangorflateTilsagnFilterOrderBy.TILTAK,
    arrangorNavn: ArrangorflateTilsagnFilterOrderBy.ARRANGOR,
    startDato: ArrangorflateTilsagnFilterOrderBy.START_DATO,
    sluttDato: ArrangorflateTilsagnFilterOrderBy.SLUTT_DATO,
    tilsagnNavn: ArrangorflateTilsagnFilterOrderBy.TILSAGN,
    status: ArrangorflateTilsagnFilterOrderBy.STATUS,
  };

  return (
    <>
      <Box paddingBlock="space-16" width="30rem">
        <Search
          label="Søk i tilsagn"
          description="Tiltaksnavn, arrangør, periode, tilsagn"
          hideLabel={false}
          variant="simple"
          width="30rem"
          onChange={setSearch}
          onClear={clearSearch}
        />
      </Box>
      <Tabellvisning
        kolonner={tilsagnKolonner}
        sort={filterToSortState(filter, sortKeyToParam, "tiltaksNavn")}
        onSortChange={(key) =>
          setFilter((old: ArrangorflateTilsagnFilter) => ({
            ...old,
            ...nextSort(filter, sortKeyToParam[key]),
          }))
        }
        pagination={paginationProps}
      >
        <Suspense fallback={<Laster tekst="Laster data..." size="xlarge" />}>
          {paginertTilsagnRader.data.map((rad, i) => (
            <TilsagnRow key={rad.id + i} row={rad} />
          ))}
        </Suspense>
      </Tabellvisning>
      {paginertTilsagnRader.data.length === 0 && <IngenTreff type="tilsagn" />}
    </>
  );
}

function useTableSearch(updateSearch: (value: string) => void) {
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebounce(search, 300);

  useEffect(() => {
    updateSearch(debouncedSearch);
  }, [debouncedSearch, updateSearch]);

  return [search, setSearch, () => setSearch("")] as const;
}

type SortableFilter<TOrderBy extends string> = {
  orderBy?: TOrderBy;
  direction?: ArrangorflateFilterDirection;
};

function filterToSortState<TOrderBy extends string, TFilter extends SortableFilter<TOrderBy>>(
  filter: TFilter,
  sortKeyToParam: Record<string, TOrderBy>,
  defaultSortKey: string,
): SortState {
  const paramToSortKey = flipObject(sortKeyToParam);
  const paramToSortDirection = flipObject({
    ascending: ArrangorflateFilterDirection.ASC,
    descending: ArrangorflateFilterDirection.DESC,
    none: ArrangorflateFilterDirection.ASC,
  });

  return {
    orderBy: (filter.orderBy && paramToSortKey[filter.orderBy]) || defaultSortKey,
    direction: paramToSortDirection[filter.direction ?? ArrangorflateFilterDirection.ASC],
  };
}

function nextSort<TOrderBy extends string>(
  filter: SortableFilter<TOrderBy>,
  orderBy: TOrderBy,
): Pick<SortableFilter<TOrderBy>, "orderBy" | "direction"> {
  if (orderBy === filter.orderBy) {
    return {
      direction:
        filter.direction === ArrangorflateFilterDirection.ASC
          ? ArrangorflateFilterDirection.DESC
          : ArrangorflateFilterDirection.ASC,
    };
  }

  return { orderBy, direction: ArrangorflateFilterDirection.ASC };
}
