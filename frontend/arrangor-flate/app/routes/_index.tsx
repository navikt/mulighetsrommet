import { Link as ReactRouterLink } from "react-router";
import { Box, Tabs, Button, HStack, PaginationProps, Search, SortState } from "@navikt/ds-react";
import {
  ArrangorflateTilsagnFilterOrderBy,
  ArrangorflateFilterDirection,
  ArrangorflateUtbetalingFilterOrderBy,
  ArrangorflateFilterType,
} from "api-client";
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
import {
  ArrangorflateUtbetalingFilter,
  useArrangorflateUtbetalingRader,
} from "~/hooks/useArrangorflateUtbetalingRader";
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
            <Suspense fallback={<Laster tekst="Laster data..." size="xlarge" />}>
              <TilsagnTabellContent />
            </Suspense>
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
  const [sok, setSok] = useState("");
  const debouncedSok = useDebounce(sok, 300);
  const {
    data: paginertUtbetalingRader,
    filter,
    setFilter,
    oppdaterSok,
  } = useArrangorflateUtbetalingRader({ type });

  useEffect(() => {
    oppdaterSok(debouncedSok);
  }, [debouncedSok, oppdaterSok]);

  function clearSearch() {
    setSok("");
  }

  const utbetalingSortKeyToParam: Record<string, ArrangorflateUtbetalingFilterOrderBy> = {
    tiltakNavn: ArrangorflateUtbetalingFilterOrderBy.TILTAK,
    arrangorNavn: ArrangorflateUtbetalingFilterOrderBy.ARRANGOR,
    startDato: ArrangorflateUtbetalingFilterOrderBy.PERIODE,
    belop: ArrangorflateUtbetalingFilterOrderBy.BELOP,
    status: ArrangorflateUtbetalingFilterOrderBy.STATUS,
  };

  const paramToSortKey: Record<ArrangorflateUtbetalingFilterOrderBy, string> =
    flipObject(utbetalingSortKeyToParam);

  const paramToSortDirection: Record<ArrangorflateFilterDirection, SortState["direction"]> =
    flipObject({
      ascending: ArrangorflateFilterDirection.ASC,
      descending: ArrangorflateFilterDirection.DESC,
      none: ArrangorflateFilterDirection.ASC,
    });

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

  function sortChange(orderBy: ArrangorflateUtbetalingFilterOrderBy) {
    if (orderBy == filter.orderBy) {
      const direction =
        filter.direction == ArrangorflateFilterDirection.ASC
          ? ArrangorflateFilterDirection.DESC
          : ArrangorflateFilterDirection.ASC;
      return setFilter((old) => ({ ...old, direction }));
    }

    setFilter((old) => ({
      ...old,
      orderBy,
      direction: ArrangorflateFilterDirection.ASC,
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
        sort={filterToSortState(filter)}
        onSortChange={(key) => sortChange(utbetalingSortKeyToParam[key])}
        pagination={paginationProps}
      >
        <Suspense fallback={<Laster tekst="Laster data..." size="xlarge" />}>
          {paginertUtbetalingRader.data.map((rad, i) => (
            <UtbetalingRow key={rad.gjennomforingId + i} row={rad} />
          ))}
        </Suspense>
      </Tabellvisning>
      {paginertUtbetalingRader.data.length === 0 && <IngenTreff type="utbetaling" />}
    </>
  );
}

function TilsagnTabellContent() {
  const { data: paginertTilsagnRader, filter, setFilter } = useArrangorflateTilsagnRader();

  const paginationProps: PaginationProps = {
    hidden: !paginertTilsagnRader.pagination.totalPages,
    page: filter.page || 1,
    count: paginertTilsagnRader.pagination.totalPages || 1,
    boundaryCount: 1,
    prevNextTexts: true,
    onPageChange: (newPage) =>
      setFilter((filter: ArrangorflateTilsagnFilter) => ({ ...filter, page: newPage })),
  };

  function sortChange(orderBy: ArrangorflateTilsagnFilterOrderBy) {
    if (orderBy == filter.orderBy) {
      const direction =
        filter.direction == ArrangorflateFilterDirection.ASC
          ? ArrangorflateFilterDirection.DESC
          : ArrangorflateFilterDirection.ASC;
      return setFilter((old: ArrangorflateTilsagnFilter) => ({ ...old, direction }));
    }

    setFilter((old: ArrangorflateTilsagnFilter) => ({
      ...old,
      orderBy,
      direction: ArrangorflateFilterDirection.ASC,
    }));
  }

  const tilsagnSortKeyToParam: Record<string, ArrangorflateTilsagnFilterOrderBy> = {
    tiltakNavn: ArrangorflateTilsagnFilterOrderBy.TILTAK,
    arrangorNavn: ArrangorflateTilsagnFilterOrderBy.ARRANGOR,
    startDato: ArrangorflateTilsagnFilterOrderBy.PERIODE,
    tilsagn: ArrangorflateTilsagnFilterOrderBy.TILSAGN,
    status: ArrangorflateTilsagnFilterOrderBy.STATUS,
  };

  const paramToSortKey: Record<ArrangorflateTilsagnFilterOrderBy, string> =
    flipObject(tilsagnSortKeyToParam);

  const paramToSortDirection: Record<ArrangorflateFilterDirection, SortState["direction"]> =
    flipObject({
      ascending: ArrangorflateFilterDirection.ASC,
      descending: ArrangorflateFilterDirection.DESC,
      none: ArrangorflateFilterDirection.ASC,
    });

  function filterToSortState({ orderBy, direction }: ArrangorflateTilsagnFilter): SortState {
    const newOrderBy: SortState["orderBy"] = (orderBy && paramToSortKey[orderBy]) || "tiltaksNavn";
    const newDirection: SortState["direction"] =
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      (direction && paramToSortDirection[direction]) || "ascending";

    return {
      orderBy: newOrderBy,
      direction: newDirection,
    };
  }

  return (
    <>
      <Tabellvisning
        kolonner={tilsagnKolonner}
        sort={filterToSortState(filter)}
        onSortChange={(key) => sortChange(tilsagnSortKeyToParam[key])}
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
