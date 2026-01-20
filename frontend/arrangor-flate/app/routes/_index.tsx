import { Box, Tabs, Button, HStack, Alert } from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrangorflateTilsagnOversikt,
  TabelloversiktRadDto,
  UtbetalingOversiktType,
} from "api-client";
import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { PageHeading } from "~/components/common/PageHeading";
import { getTabStateOrDefault, useTabState } from "~/hooks/useTabState";
import { tekster } from "~/tekster";
import { problemDetailResponse } from "~/utils/validering";
import css from "../root.module.css";
import { DataDrivenTable, useSortableData } from "@mr/frontend-common";
import { pathTo } from "~/utils/navigation";
import { Tabellvisning } from "~/components/common/Tabellvisning";
import { UtbetalingRow } from "~/components/common/UtbetalingRow";

export const meta: MetaFunction = () => {
  return [
    { title: "Utbetalinger til tiltaksarrangør" },
    { name: "description", content: "Arrangørflate for krav om utbetalinger" },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  const { data: tilsagn, error: tilsagnError } =
    await ArrangorflateService.getArrangorflateTilsagnOversikt({
      headers: await apiHeaders(request),
    });
  if (tilsagnError) {
    throw problemDetailResponse(tilsagnError);
  }

  const tabState = getTabStateOrDefault(request);
  const utbetalingType =
    tabState === "aktive" ? UtbetalingOversiktType.AKTIVE : UtbetalingOversiktType.HISTORISKE;
  const [{ data, error: utbetalingerError }] = await Promise.all([
    ArrangorflateService.getArrangorflateUtbetalinger({
      query: { type: utbetalingType },
      headers: await apiHeaders(request),
    }),
  ]);
  if (utbetalingerError) {
    throw problemDetailResponse(utbetalingerError);
  }

  return { data, tilsagn };
}

export default function Oversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const { data, tilsagn } = useLoaderData<typeof loader>();

  const { sortedData, sort, toggleSort } = useSortableData<TabelloversiktRadDto, undefined, string>(
    data,
  );
  return (
    <Box className={css.side}>
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
        </Tabs.List>
        <Tabs.Panel value={currentTab}>
          {currentTab === "tilsagn" ? (
            <TilsagnTabell tilsagnOversikt={tilsagn} />
          ) : (
            <Tabellvisning kolonner={utbetalingKolonner} sort={sort} onSortChange={toggleSort}>
              {sortedData.map((rad) => (
                <UtbetalingRow key={rad.gjennomforingId} row={rad} periode />
              ))}
            </Tabellvisning>
          )}
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}

const utbetalingKolonner: Array<{ key: string; label: string }> = [
  { key: "tiltakNavn", label: "Tiltak" },
  { key: "arrangorNavn", label: "Arrangør" },
  { key: "periode", label: "Periode" },
  { key: "belop", label: "Beløp" },
  { key: "type", label: "Type" },
  { key: "status", label: "Status" },
];

interface TilsagnTabellProps {
  tilsagnOversikt: ArrangorflateTilsagnOversikt;
}

function TilsagnTabell({ tilsagnOversikt }: TilsagnTabellProps) {
  if (!tilsagnOversikt.tabell) {
    return (
      <Alert className="my-10" variant="info">
        Det finnes ingen tilsagn her
      </Alert>
    );
  }
  return <DataDrivenTable data={tilsagnOversikt.tabell} zebraStripes />;
}
