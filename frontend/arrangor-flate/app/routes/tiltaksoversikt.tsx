import { BodyShort, Box, Heading, Tabs, Link, VStack, LocalAlert } from "@navikt/ds-react";
import { ArrangorflateService, ArrangorInnsendingRadDto, TiltaksoversiktType } from "api-client";
import {
  Link as ReactRouterLink,
  LoaderFunctionArgs,
  MetaFunction,
  useLoaderData,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { problemDetailResponse } from "~/utils/validering";
import { tekster } from "~/tekster";
import { getTabStateOrDefault, useTabState } from "~/hooks/useTabState";
import { Tabellvisning } from "~/components/common/Tabellvisning";
import { useSortableData } from "@mr/frontend-common";
import { UtbetalingRow } from "~/components/common/UtbetalingRow";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { pathTo } from "~/utils/navigation";

export const meta: MetaFunction = () => {
  return [
    { title: "Tiltaksoversikt - Opprett krav om utbetaling" },
    {
      name: "description",
      content: "Velg et tiltak for å opprette krav om utbetaling",
    },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  const tabState = getTabStateOrDefault(request);
  const type = tabState === "aktive" ? TiltaksoversiktType.AKTIVE : TiltaksoversiktType.HISTORISKE;

  const { data, error } = await ArrangorflateService.getArrangorTiltaksoversikt({
    query: { type },
    headers: await apiHeaders(request),
  });
  if (error) throw problemDetailResponse(error);

  return { data } as const;
}

export default function OpprettKravTiltaksOversikt() {
  const { data } = useLoaderData<typeof loader>();
  const [currentTab, setTab] = useTabState();

  const { sortedData, sort, toggleSort } = useSortableData<
    ArrangorInnsendingRadDto,
    undefined,
    string
  >(data);

  return (
    <Box background="default" padding="space-16" borderRadius="8">
      <VStack gap="space-16">
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
            {sortedData.length === 0 ? (
              <Box marginBlock="space-16">
                <LocalAlert status="warning" className="my-10">
                  <LocalAlert.Header>
                    <LocalAlert.Title>Fant ingen registrerte tiltak</LocalAlert.Title>
                  </LocalAlert.Header>
                  <LocalAlert.Content>
                    <BodyShort spacing>
                      Det finnes ingen registrerte tiltak du kan sende inn utbetalingskrav for.
                    </BodyShort>
                    <BodyShort>Ta eventuelt kontakt med Nav ved behov.</BodyShort>
                  </LocalAlert.Content>
                </LocalAlert>
              </Box>
            ) : (
              <Tabellvisning kolonner={kolonner} sort={sort} onSortChange={toggleSort}>
                {sortedData.map((row: ArrangorInnsendingRadDto) => (
                  <UtbetalingRow key={row.gjennomforingId} row={row} />
                ))}
              </Tabellvisning>
            )}
          </Tabs.Panel>
        </Tabs>
      </VStack>
    </Box>
  );
}

const kolonner: Array<{ key: string; label: string }> = [
  { key: "tiltakNavn", label: "Tiltak" },
  { key: "arrangorNavn", label: "Arrangør" },
  { key: "startDato", label: "Periode" },
];
