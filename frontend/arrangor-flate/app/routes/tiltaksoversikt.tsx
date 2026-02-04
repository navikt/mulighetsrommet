import { Alert, BodyShort, Box, Heading, Tabs, Link, VStack } from "@navikt/ds-react";
import { ArrangorInnsendingRadDto, TiltaksoversiktType } from "api-client";
import { Suspense } from "react";
import { Link as ReactRouterLink, MetaFunction } from "react-router";
import { tekster } from "~/tekster";
import { useTabState } from "~/hooks/useTabState";
import { Tabellvisning } from "~/components/common/Tabellvisning";
import { useSortableData } from "@mr/frontend-common";
import { UtbetalingRow } from "~/components/common/UtbetalingRow";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { pathTo } from "~/utils/navigation";
import { Laster } from "~/components/common/Laster";
import { useArrangorTiltaksoversikt } from "~/hooks/useArrangorflateTiltaksoversikt";

export const meta: MetaFunction = () => {
  return [
    { title: "Tiltaksoversikt - Opprett krav om utbetaling" },
    {
      name: "description",
      content: "Velg et tiltak for å opprette krav om utbetaling",
    },
  ];
};

export default function OpprettKravTiltaksOversikt() {
  const [currentTab, setTab] = useTabState();

  return (
    <Box background="bg-default" padding="4" borderRadius="large">
      <VStack gap="4">
        <Link as={ReactRouterLink} to={pathTo.utbetalinger} className="max-w-max">
          <ChevronLeftIcon /> Tilbake til oversikt
        </Link>
        <Heading level="2" size="large">
          {tekster.bokmal.gjennomforing.headingTitle}
        </Heading>
        <Tabs
          defaultValue={currentTab}
          onChange={(tab) => {
            setTab(tab as "aktive" | "historiske");
          }}
        >
          <Tabs.List>
            <Tabs.Tab value="aktive" label={tekster.bokmal.gjennomforing.oversiktFaner.aktive} />
            <Tabs.Tab
              value="historiske"
              label={tekster.bokmal.gjennomforing.oversiktFaner.historiske}
            />
          </Tabs.List>
          <Tabs.Panel value={currentTab}>
            <Suspense fallback={<Laster tekst="Laster tiltak..." size="xlarge" />}>
              <TiltaksOversiktContent
                type={
                  currentTab === "aktive"
                    ? TiltaksoversiktType.AKTIVE
                    : TiltaksoversiktType.HISTORISKE
                }
              />
            </Suspense>
          </Tabs.Panel>
        </Tabs>
      </VStack>
    </Box>
  );
}

function TiltaksOversiktContent({ type }: { type: TiltaksoversiktType }) {
  const { data } = useArrangorTiltaksoversikt(type);

  const { sortedData, sort, toggleSort } = useSortableData(data);

  if (sortedData.length === 0) {
    return (
      <Box marginBlock="4">
        <Alert variant="info">
          <BodyShort>
            Det finnes ingen registrerte tiltak du kan sende inn utbetalingskrav for.
          </BodyShort>
          <BodyShort>Ta eventuelt kontakt med Nav ved behov.</BodyShort>
        </Alert>
      </Box>
    );
  }

  return (
    <Tabellvisning kolonner={kolonner} sort={sort} onSortChange={toggleSort}>
      {sortedData.map((row: ArrangorInnsendingRadDto) => (
        <UtbetalingRow key={row.gjennomforingId} row={row} />
      ))}
    </Tabellvisning>
  );
}

const kolonner: Array<{ key: string; label: string }> = [
  { key: "tiltakNavn", label: "Tiltak" },
  { key: "arrangorNavn", label: "Arrangør" },
  { key: "startDato", label: "Periode" },
];
