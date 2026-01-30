import { Alert, BodyShort, Box, Heading, Tabs, Link, VStack } from "@navikt/ds-react";
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
import { useFileStorage } from "~/hooks/useFileStorage";
import { useEffect } from "react";
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
  const storage = useFileStorage();
  const { data } = useLoaderData<typeof loader>();
  const [currentTab, setTab] = useTabState();

  useEffect(() => {
    storage.clear();
  }, [storage]);

  const { sortedData, sort, toggleSort } = useSortableData<
    ArrangorInnsendingRadDto,
    undefined,
    string
  >(data);

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
            {sortedData.length === 0 ? (
              <Box marginBlock="4">
                <Alert variant="info">
                  <BodyShort>
                    Det finnes ingen registrerte tiltak du kan sende inn utbetalingskrav for.
                  </BodyShort>
                  <BodyShort>Ta eventuelt kontakt med Nav ved behov.</BodyShort>
                </Alert>
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
