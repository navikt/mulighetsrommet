import { Box, Tabs, Button, HStack, Alert } from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrangorflateTilsagnRadDto,
  ArrangorInnsendingRadDto,
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
import { useSortableData } from "@mr/frontend-common";
import { pathTo } from "~/utils/navigation";
import { Tabellvisning } from "~/components/common/Tabellvisning";
import { utbetalingKolonner, UtbetalingRow } from "~/components/common/UtbetalingRow";
import { tilsagnKolonner, TilsagnRow } from "~/components/common/TilsagnRow";

export const meta: MetaFunction = () => {
  return [
    { title: "Utbetalinger til tiltaksarrangør" },
    { name: "description", content: "Arrangørflate for krav om utbetalinger" },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  const tabState = getTabStateOrDefault(request);

  if (tabState === "tilsagnsoversikt") {
    const { data: tilsagnRader, error: tilsagnError } =
      await ArrangorflateService.getArrangorflateTilsagnRader({
        headers: await apiHeaders(request),
      });
    if (tilsagnError) {
      throw problemDetailResponse(tilsagnError);
    }
    return { tilsagnRader, utbetalingRader: [] };
  }

  const utbetalingType =
    tabState === "aktive" ? UtbetalingOversiktType.AKTIVE : UtbetalingOversiktType.HISTORISKE;

  const [{ data: utbetalingRader, error: utbetalingerError }] = await Promise.all([
    ArrangorflateService.getArrangorflateUtbetalinger({
      query: { type: utbetalingType },
      headers: await apiHeaders(request),
    }),
  ]);
  if (utbetalingerError) {
    throw problemDetailResponse(utbetalingerError);
  }

  return { utbetalingRader, tilsagnRader: [] };
}

export default function Oversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const { utbetalingRader, tilsagnRader } = useLoaderData<typeof loader>();

  const { sortedData, sort, toggleSort } = useSortableData<
    ArrangorInnsendingRadDto,
    number | undefined,
    string
  >(utbetalingRader, undefined, (item, key) => {
    if (key === "belop") {
      return item.belop?.belop;
    }
    return (item as any)[key];
  });
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
          <Tabs.Tab
            value="tilsagnsoversikt"
            label={tekster.bokmal.utbetaling.oversiktFaner.tilsagnsoversikt}
          />
        </Tabs.List>
        <Tabs.Panel value={currentTab}>
          {currentTab === "tilsagnsoversikt" ? (
            <TilsagnTabell tilsagnRader={tilsagnRader} />
          ) : (
            <Tabellvisning kolonner={utbetalingKolonner} sort={sort} onSortChange={toggleSort}>
              {sortedData.map((rad, i) => (
                <UtbetalingRow key={rad.gjennomforingId + i} row={rad} periode />
              ))}
            </Tabellvisning>
          )}
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}

interface TilsagnTabellProps {
  tilsagnRader: ArrangorflateTilsagnRadDto[];
}

function TilsagnTabell({ tilsagnRader }: TilsagnTabellProps) {
  const { sortedData, sort, toggleSort } = useSortableData<
    ArrangorflateTilsagnRadDto,
    number | undefined,
    string
  >(tilsagnRader, undefined, (item, key) => {
    if (key === "periode") {
      return item[key].start;
    }
    return (item as any)[key];
  });
  if (!tilsagnRader.length) {
    return (
      <Alert className="my-10" variant="info">
        Det finnes ingen tilsagn her
      </Alert>
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
