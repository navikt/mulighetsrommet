import { Link as ReactRouterLink } from "react-router";
import { Box, Tabs, Button, HStack } from "@navikt/ds-react";
import { ArrangorflateUtbetalingFilterType } from "api-client";
import type { MetaFunction } from "react-router";
import { PageHeading } from "~/components/common/PageHeading";
import { useTabState } from "~/hooks/useTabState";
import { tekster } from "~/tekster";
import { pathTo } from "~/utils/navigation";
import { Suspense } from "react";
import { Laster } from "~/components/common/Laster";
import { UtbetalingTabell } from "~/components/utbetaling/UtbetalingTabell";
import { TilsagnTabell } from "~/components/tilsagn/TilsagnTabell";

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
              <TilsagnTabell />
            </Suspense>
          ) : (
            <UtbetalingTabell
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
