import { Button, Tabs } from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrFlateUtbetalingKompakt,
  ArrFlateUtbetalingStatus,
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

export const meta: MetaFunction = () => {
  return [
    { title: "Krav om utbetalinger" },
    { name: "description", content: "Arrangørflate for krav om utbetalinger" },
  ];
};

export type Tabs = "aktive" | "historiske" | "tilsagnsoversikt";

export async function loader({ request, params }: LoaderFunctionArgs) {
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }

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

  return { utbetalinger, tilsagn };
}

export default function UtbetalingOversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const { utbetalinger, tilsagn } = useLoaderData<typeof loader>();
  const historiske: ArrFlateUtbetalingKompakt[] = utbetalinger.filter(
    (k) => k.status === ArrFlateUtbetalingStatus.UTBETALT,
  );
  const aktive = utbetalinger.filter((k) => k.status !== ArrFlateUtbetalingStatus.UTBETALT);
  const orgnr = useOrgnrFromUrl();

  return (
    <>
      <div className="flex justify-between sm:flex-row sm:my-5 sm:p-1">
        <PageHeader title="Tilgjengelige innsendinger" />
        <Button
          variant="secondary"
          as={ReactRouterLink}
          to={internalNavigation(orgnr).manueltUtbetalingskrav}
        >
          Opprett manuelt krav om utbetaling
        </Button>
      </div>
      <Tabs defaultValue={currentTab} onChange={(tab) => setTab(tab as Tabs)}>
        <Tabs.List>
          <Tabs.Tab value="aktive" label="Aktive" />
          <Tabs.Tab value="historiske" label="Historiske" />
          <Tabs.Tab value="tilsagnsoversikt" label="Tilsagnsoversikt" />
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
