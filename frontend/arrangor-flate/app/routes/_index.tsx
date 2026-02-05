import { Link as ReactRouterLink } from "react-router";
import { Box, Tabs, Button, HStack, LocalAlert } from "@navikt/ds-react";
import { UtbetalingOversiktType } from "api-client";
import type { MetaFunction } from "react-router";
import { PageHeading } from "~/components/common/PageHeading";
import { useTabState } from "~/hooks/useTabState";
import { tekster } from "~/tekster";
import { useSortableData } from "@mr/frontend-common";
import { pathTo } from "~/utils/navigation";
import { Tabellvisning } from "~/components/common/Tabellvisning";
import { utbetalingKolonner, UtbetalingRow } from "~/components/common/UtbetalingRow";
import { tilsagnKolonner, TilsagnRow } from "~/components/common/TilsagnRow";
import { Suspense } from "react";
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
          <Suspense fallback={<Laster tekst="Laster data..." size="xlarge" />}>
            {currentTab === "tilsagnsoversikt" ? (
              <TilsagnTabellContent />
            ) : (
              <UtbetalingTabellContent
                type={
                  currentTab === "aktive"
                    ? UtbetalingOversiktType.AKTIVE
                    : UtbetalingOversiktType.HISTORISKE
                }
              />
            )}
          </Suspense>
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}

function UtbetalingTabellContent({ type }: { type: UtbetalingOversiktType }) {
  const { data: utbetalingRader } = useArrangorflateUtbetalinger(type);

  const { sortedData, sort, toggleSort } = useSortableData(
    utbetalingRader,
    undefined,
    (item, key) => {
      if (key === "belop") {
        return item.belop?.belop;
      }
      return (item as Record<string, unknown>)[key];
    },
  );

  return (
    <Tabellvisning kolonner={utbetalingKolonner} sort={sort} onSortChange={toggleSort}>
      {sortedData.map((rad, i) => (
        <UtbetalingRow key={rad.gjennomforingId + i} row={rad} />
      ))}
    </Tabellvisning>
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
      <LocalAlert status="warning" className="my-10">
        <LocalAlert.Header>
          <LocalAlert.Title>Det finnes ingen tilsagn her</LocalAlert.Title>
        </LocalAlert.Header>
      </LocalAlert>
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
