import { Box, Tabs as AkselTabs, Button } from "@navikt/ds-react";
import { ArrangorflateService, UtbetalingOversiktType } from "api-client";
import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { PageHeading } from "~/components/common/PageHeading";
import { getTabStateOrDefault, useTabState, Tabs } from "~/hooks/useTabState";
import { tekster } from "~/tekster";
import { problemDetailResponse } from "~/utils/validering";
import css from "../root.module.css";
import { DataDrivenTable } from "@mr/frontend-common";
import { TilsagnTable } from "~/components/tilsagn/TilsagnTable";
import { pathTo } from "~/utils/navigation";

export const meta: MetaFunction = () => {
  return [
    { title: "Utbetalinger til tiltaksarrangør" },
    { name: "description", content: "Arrangørflate for krav om utbetalinger" },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  const tabState = getTabStateOrDefault(new URL(request.url));

  if (tabState === "tilsagnsoversikt") {
    const { data: tilsagn, error: tilsagnError } =
      await ArrangorflateService.getAllArrangorflateTilsagn({
        headers: await apiHeaders(request),
      });
    if (tilsagnError) {
      throw problemDetailResponse(tilsagnError);
    }
    return {
      tilsagn,
    };
  }

  const utbetalingType =
    tabState === "aktive" ? UtbetalingOversiktType.AKTIVE : UtbetalingOversiktType.HISTORISKE;
  const [{ data: utbetalinger, error: utbetalingerError }] = await Promise.all([
    ArrangorflateService.getArrangorflateUtbetalinger({
      query: { type: utbetalingType },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingerError) {
    throw problemDetailResponse(utbetalingerError);
  }

  if (tabState === "aktive") {
    return {
      aktive: utbetalinger,
    };
  }
  return {
    historiske: utbetalinger,
  };
}

export default function Oversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const { aktive, historiske, tilsagn } = useLoaderData<typeof loader>();

  return (
    <Box className={css.side}>
      <div className="flex justify-between sm:flex-row sm:p-1">
        <PageHeading title={tekster.bokmal.utbetaling.headingTitle} />
        <OpprettManueltUtbetalingskrav />
      </div>
      <AkselTabs defaultValue={currentTab} onChange={(tab) => setTab(tab as Tabs)}>
        <AkselTabs.List>
          <AkselTabs.Tab value="aktive" label={tekster.bokmal.utbetaling.oversiktFaner.aktive} />
          <AkselTabs.Tab
            value="historiske"
            label={tekster.bokmal.utbetaling.oversiktFaner.historiske}
          />
          <AkselTabs.Tab
            value="tilsagnsoversikt"
            label={tekster.bokmal.utbetaling.oversiktFaner.tilsagnsoversikt}
          />
        </AkselTabs.List>
        <AkselTabs.Panel value="aktive" className="w-full">
          {aktive?.tabell && <DataDrivenTable data={aktive.tabell} zebraStripes />}
        </AkselTabs.Panel>
        <AkselTabs.Panel value="historiske" className="w-full">
          {historiske?.tabell && <DataDrivenTable data={historiske.tabell} zebraStripes />}
        </AkselTabs.Panel>
        <AkselTabs.Panel value="tilsagnsoversikt" className="w-full">
          {tilsagn && <TilsagnTable tilsagn={tilsagn} />}
        </AkselTabs.Panel>
      </AkselTabs>
    </Box>
  );
}

function OpprettManueltUtbetalingskrav() {
  return (
    <Button variant="secondary" as={ReactRouterLink} to={pathTo.tiltaksoversikt}>
      {tekster.bokmal.utbetaling.opprettUtbetaling.actionLabel}
    </Button>
  );
}
