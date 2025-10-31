import { ChevronDownIcon } from "@navikt/aksel-icons";
import { ActionMenu, Box, Button, Tabs } from "@navikt/ds-react";
import { ArrangorflateService, FeatureToggle } from "api-client";
import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { PageHeading } from "~/components/common/PageHeading";
import { TilsagnTable } from "~/components/tilsagn/TilsagnTable";
import { UtbetalingTable } from "~/components/utbetaling/UtbetalingTable";
import { useTabState } from "~/hooks/useTabState";
import { toggleIsEnabled } from "~/services/featureToggle/featureToggleService";
import { tekster } from "~/tekster";
import { pathByOrgnr, useOrgnrFromUrl } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import css from "../root.module.css";

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

  const opprettUtbetalingsKravAnnenAvtaltPrisToggle = await toggleIsEnabled({
    orgnr,
    feature: FeatureToggle.ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS,
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

  if (utbetalingerError) {
    throw problemDetailResponse(utbetalingerError);
  }
  if (tilsagnError) {
    throw problemDetailResponse(tilsagnError);
  }
  return {
    aktive: utbetalinger.aktive,
    historiske: utbetalinger.historiske,
    kanOppretteManueltKrav: utbetalinger.kanOppretteManueltKrav,
    tilsagn,
    opprettUtbetalingsKravAnnenAvtaltPrisToggle,
  };
}

export default function UtbetalingOversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const {
    aktive,
    historiske,
    tilsagn,
    kanOppretteManueltKrav,
    opprettUtbetalingsKravAnnenAvtaltPrisToggle,
  } = useLoaderData<typeof loader>();
  const orgnr = useOrgnrFromUrl();

  return (
    <Box className={css.side}>
      <div className="flex justify-between sm:flex-row sm:p-1">
        <PageHeading title={tekster.bokmal.utbetaling.headingTitle} />
        <OpprettManueltUtbetalingskrav
          orgnr={orgnr}
          kanOppretteManueltKrav={kanOppretteManueltKrav}
          opprettUtbetalingsKravAnnenAvtaltPrisToggle={opprettUtbetalingsKravAnnenAvtaltPrisToggle}
        />
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
          <UtbetalingTable utbetalinger={aktive} belopColumn="innsendt" />
        </Tabs.Panel>
        <Tabs.Panel value="historiske" className="w-full">
          <UtbetalingTable utbetalinger={historiske} belopColumn="godkjent" />
        </Tabs.Panel>
        <Tabs.Panel value="tilsagnsoversikt" className="w-full">
          <TilsagnTable tilsagn={tilsagn} />
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}

interface OpprettManueltUtbetalingskravProps {
  orgnr: string;
  kanOppretteManueltKrav: boolean;
  opprettUtbetalingsKravAnnenAvtaltPrisToggle: boolean;
}

function OpprettManueltUtbetalingskrav({
  orgnr,
  kanOppretteManueltKrav,
  opprettUtbetalingsKravAnnenAvtaltPrisToggle,
}: OpprettManueltUtbetalingskravProps) {
  if (kanOppretteManueltKrav) {
    return (
      <Button
        variant="secondary"
        as={ReactRouterLink}
        to={pathByOrgnr(orgnr).opprettKrav.tiltaksOversikt}
      >
        {tekster.bokmal.utbetaling.opprettUtbetaling.actionLabel}
      </Button>
    );
  }
  return (
    <ActionMenu>
      <ActionMenu.Trigger>
        <Button variant="secondary" icon={<ChevronDownIcon aria-hidden />} iconPosition="right">
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
  );
}
