import { Link as ReactRouterLink } from "react-router";
import { Box, Tabs, Button, HStack, InfoCard, PaginationProps, Search } from "@navikt/ds-react";
import { ArrangorflateUtbetalingFilterType } from "api-client";
import type { MetaFunction } from "react-router";
import { PageHeading } from "~/components/common/PageHeading";
import { useTabState } from "~/hooks/useTabState";
import { tekster } from "~/tekster";
import { useDebounce, useSortableData } from "@mr/frontend-common";
import { pathTo } from "~/utils/navigation";
import { Tabellvisning } from "~/components/common/Tabellvisning";
import { utbetalingKolonner, UtbetalingRow } from "~/components/common/UtbetalingRow";
import { tilsagnKolonner, TilsagnRow } from "~/components/common/TilsagnRow";
import { Suspense, useEffect, useState } from "react";
import { Laster } from "~/components/common/Laster";
import { useArrangorflateTilsagnRader } from "~/hooks/useArrangorflateTilsagnRader";
import { useArrangorflateUtbetalinger } from "~/hooks/useArrangorflateUtbetalinger";

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
      <Tabs defaultValue={currentTab} onChange={(tab) => setTab(tab as "aktive" | "historiske")}>
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
              type={
                currentTab === "aktive"
                  ? ArrangorflateUtbetalingFilterType.AKTIVE
                  : ArrangorflateUtbetalingFilterType.HISTORISKE
              }
            />
          )}
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}

function UtbetalingTabellContent({ type }: { type: ArrangorflateUtbetalingFilterType }) {
  const [sok, setSok] = useState("");
  const debouncedSok = useDebounce(sok);
  const {
    data: paginertUtbetalingRader,
    filter,
    setFilter,
  } = useArrangorflateUtbetalinger({ type });

  useEffect(() => {
    setFilter((filter) => ({ ...filter, sok: debouncedSok.trim() }));
  }, [debouncedSok, setFilter]);

  const { sortedData, sort, toggleSort } = useSortableData(
    paginertUtbetalingRader.data,
    undefined,
    (item, key) => {
      if (key === "belop") {
        return item.belop?.belop;
      }
      return (item as Record<string, unknown>)[key];
    },
  );
  const paginationProps: PaginationProps = {
    hidden: !paginertUtbetalingRader.pagination.totalPages,
    page: filter?.page || 1,
    count: paginertUtbetalingRader.pagination.totalPages || 1,
    boundaryCount: 1,
    prevNextTexts: true,
    onPageChange: (newPage) => setFilter((filter) => ({ ...filter, page: newPage })),
  };

  return (
    <>
      <form role="search">
        <Search
          label="Søk i alle Nav sine sider"
          variant="simple"
          value={sok}
          onChange={setSok}
          onClear={() => setSok("")}
        />
      </form>
      <Tabellvisning
        kolonner={utbetalingKolonner}
        sort={sort}
        onSortChange={toggleSort}
        pagination={paginationProps}
      >
        <Suspense fallback={<Laster tekst="Laster data..." size="xlarge" />}>
          {sortedData.map((rad, i) => (
            <UtbetalingRow key={rad.gjennomforingId + i} row={rad} />
          ))}
        </Suspense>
      </Tabellvisning>
    </>
  );
}

function TilsagnTabellContent() {
  const { data: tilsagnRader } = useArrangorflateTilsagnRader();

  const { sortedData, sort, toggleSort } = useSortableData(tilsagnRader, undefined, (item, key) => {
    if (key === "periode") {
      return item[key].start;
    }
    return (item as Record<string, unknown>)[key];
  });

  if (!tilsagnRader.length) {
    return (
      <InfoCard data-color="warning" className="my-10">
        <InfoCard.Header>
          <InfoCard.Title>Det finnes ingen tilsagn her</InfoCard.Title>
        </InfoCard.Header>
      </InfoCard>
    );
  }

  return (
    <Tabellvisning kolonner={tilsagnKolonner} sort={sort} onSortChange={toggleSort}>
      {sortedData.map((rad) => (
        <TilsagnRow key={rad.id} row={rad} />
      ))}
    </Tabellvisning>
  );
}
