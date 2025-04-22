import {
  ArrangorflateService,
  ArrFlateUtbetalingKompakt,
  ArrFlateUtbetalingStatus,
} from "api-client";
import { Button, HStack, Tabs } from "@navikt/ds-react";
import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { UtbetalingTable } from "~/components/utbetaling/UtbetalingTable";
import { TilsagnTable } from "~/components/tilsagn/TilsagnTable";
import { PageHeader } from "../components/PageHeader";
import { useTabState } from "../hooks/useTabState";
import { apiHeaders } from "~/auth/auth.server";
import { problemDetailResponse, useOrgnrFromUrl } from "~/utils";
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
      <HStack justify={"space-between"}>
        <PageHeader title="Tilgjengelige innsendinger" />
        <Button
          variant="secondary"
          as={ReactRouterLink}
          to={internalNavigation(orgnr).manueltUtbetalingskrav}
        >
          Opprett manuelt krav om utbetaling
        </Button>
      </HStack>
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
