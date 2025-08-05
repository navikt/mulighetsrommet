import { ActionMenu, Box, Button, Tabs } from "@navikt/ds-react";
import { ArrangorflateService, Toggles } from "api-client";
import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnTable } from "~/components/tilsagn/TilsagnTable";
import { UtbetalingTable } from "~/components/utbetaling/UtbetalingTable";
import { useTabState } from "~/hooks/useTabState";
import { toggleIsEnabled } from "~/services/featureToggle/featureToggleService";
import { tekster } from "~/tekster";
import css from "../root.module.css";
import { pathByOrgnr, useOrgnrFromUrl } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import { PageHeading } from "~/components/common/PageHeading";
import { ChevronDownIcon } from "@navikt/aksel-icons";

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

  const opprettKravOmUtbetalingToggle = await toggleIsEnabled({
    orgnr,
    feature: Toggles.ARRANGORFLATE_OPPRETT_UTBETEALING_INVESTERINGER,
    tiltakskoder: [],
    headers: await apiHeaders(request),
  });
  const opprettUtbetalingsKravAnnenAvtaltPrisToggle = await toggleIsEnabled({
    orgnr,
    feature: Toggles.ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS,
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
  return {
    aktive: utbetalinger.aktive,
    historiske: utbetalinger.historiske,
    tilsagn,
    opprettKravOmUtbetalingToggle,
    opprettUtbetalingsKravAnnenAvtaltPrisToggle,
  };
}

export default function UtbetalingOversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const {
    aktive,
    historiske,
    tilsagn,
    opprettKravOmUtbetalingToggle,
    opprettUtbetalingsKravAnnenAvtaltPrisToggle,
  } = useLoaderData<typeof loader>();
  const orgnr = useOrgnrFromUrl();

  return (
    <Box className={css.side}>
      <div className="flex justify-between sm:flex-row sm:p-1">
        <PageHeading title={tekster.bokmal.utbetaling.headingTitle} />
        {opprettKravOmUtbetalingToggle && (
          <>
            <ActionMenu>
              <ActionMenu.Trigger>
                <Button
                  variant="secondary"
                  icon={<ChevronDownIcon aria-hidden />}
                  iconPosition="right"
                >
                  {tekster.bokmal.utbetaling.opprettUtbetaling.actionLabel}
                </Button>
              </ActionMenu.Trigger>
              <ActionMenu.Content>
                <ActionMenu.Group label="Utbetalingskrav">
                  {opprettUtbetalingsKravAnnenAvtaltPrisToggle && (
                    <ActionMenu.Item
                      as={ReactRouterLink}
                      to={pathByOrgnr(orgnr).opprettKrav.driftstilskudd.innsendingsinformasjon}
                    >
                      {tekster.bokmal.utbetaling.opprettUtbetaling.driftstilskudd}
                    </ActionMenu.Item>
                  )}

                  <ActionMenu.Item
                    as={ReactRouterLink}
                    to={pathByOrgnr(orgnr).opprettKravInnsendingsinformasjon}
                  >
                    {tekster.bokmal.utbetaling.opprettUtbetaling.investering}
                  </ActionMenu.Item>
                </ActionMenu.Group>
              </ActionMenu.Content>
            </ActionMenu>
          </>
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
    </Box>
  );
}
