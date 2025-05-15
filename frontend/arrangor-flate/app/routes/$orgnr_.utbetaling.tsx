import { Button, Tabs } from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrFlateUtbetalingKompakt,
  ArrFlateUtbetalingStatus,
  Toggles,
} from "api-client";
import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnTable } from "~/components/tilsagn/TilsagnTable";
import { UtbetalingTable } from "~/components/utbetaling/UtbetalingTable";
import { problemDetailResponse, useOrgnrFromUrl } from "~/utils";
import { PageHeader } from "../components/PageHeader";
import { useTabState } from "../hooks/useTabState";
import { internalNavigation } from "../internal-navigation";
import { toggleIsEnabled } from "../services/featureToggle/featureToggleService";
import { tekster } from "../tekster";

export const meta: MetaFunction = () => {
  return [
    { title: "Oversikt" },
    { name: "description", content: "Arrangørflate for krav om utbetalinger" },
  ];
};

export type Tabs = "aktive" | "historiske" | "tilsagnsoversikt";

export async function loader({ request, params }: LoaderFunctionArgs) {
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }

  const opprettManuellUtbetalingToggle = await toggleIsEnabled({
    orgnr,
    feature: Toggles.ARRANGORFLATE_UTBETALING_OPPRETT_UTBETALING_KNAPP,
    tiltakskoder: [],
    headers: await apiHeaders(request),
  });

  const [{ data: utbetalinger, error: utbetalingerError }, { data: tilsagn, error: tilsagnError }] =
    await Promise.all([
      ArrangorflateService.getAllUtbetaling({
        path: { orgnr },
        headers: await apiHeaders(request),
      }),
      ArrangorflateService.getAllArrangorflateTilsagn({
        path: { orgnr },
        headers: await apiHeaders(request),
      }),
    ]);

  if (utbetalingerError || !utbetalinger) {
    throw problemDetailResponse(utbetalingerError);
  }
  if (tilsagnError || !tilsagn) {
    throw problemDetailResponse(tilsagnError);
  }
  return { utbetalinger, tilsagn, opprettManuellUtbetalingToggle };
}

export default function UtbetalingOversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const { utbetalinger, tilsagn, opprettManuellUtbetalingToggle } = useLoaderData<typeof loader>();
  const historiske: ArrFlateUtbetalingKompakt[] = utbetalinger.filter(
    (k) => k.status === ArrFlateUtbetalingStatus.UTBETALT,
  );
  const aktive = utbetalinger.filter((k) => k.status !== ArrFlateUtbetalingStatus.UTBETALT);
  const orgnr = useOrgnrFromUrl();

  return (
    <>
      <div className="flex justify-between sm:flex-row sm:my-5 sm:p-1">
        <PageHeader title={tekster.bokmal.utbetaling.headingTitle} />
        {opprettManuellUtbetalingToggle && (
          <Button
            variant="secondary"
            as={ReactRouterLink}
            to={internalNavigation(orgnr).manueltUtbetalingskrav}
          >
            {tekster.bokmal.utbetaling.opprettUtbetalingKnapp}
          </Button>
        )}
      </div>
      <Tabs defaultValue={currentTab} onChange={(tab) => setTab(tab as Tabs)}>
        <Tabs.List>
          <Tabs.Tab value="aktive" label={tekster.bokmal.utbetaling.oversiktFaner.aktive} />
          <Tabs.Tab value="historiske" label={tekster.bokmal.utbetaling.oversiktFaner.historiske} />
          <Tabs.Tab
            value="tilsagnsoversikt"
            label={tekster.bokmal.utbetaling.oversiktFaner.tilsagnsoversikt}
          />
        </Tabs.List>
        <Tabs.Panel value="aktive" className="w-full">
          <UtbetalingTable utbetalinger={aktive} />
        </Tabs.Panel>
        <Tabs.Panel value="historiske" className="w-full">
          <UtbetalingTable utbetalinger={historiske} />
        </Tabs.Panel>
        <Tabs.Panel value="tilsagnsoversikt" className="w-full">
          <TilsagnTable tilsagn={tilsagn} />
        </Tabs.Panel>
      </Tabs>
    </>
  );
}
