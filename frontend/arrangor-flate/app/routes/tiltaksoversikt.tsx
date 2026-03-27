import {
  InfoCard,
  BodyShort,
  Box,
  Heading,
  Tabs,
  Link,
  VStack,
  SortState,
  PaginationProps,
  Search,
} from "@navikt/ds-react";
import {
  ArrangorflateFilterDirection,
  ArrangorflateFilterType,
  ArrangorflateTiltakFilterOrderBy,
  ArrangorInnsendingRadDto,
} from "api-client";
import { Suspense, useEffect, useState } from "react";
import { Link as ReactRouterLink, MetaFunction } from "react-router";
import { tekster } from "~/tekster";
import { useTabState } from "~/hooks/useTabState";
import { Kolonne, Tabellvisning } from "~/components/common/Tabellvisning";
import { UtbetalingRow } from "~/components/common/UtbetalingRow";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { pathTo } from "~/utils/navigation";
import { Laster } from "~/components/common/Laster";
import {
  ArrangorflateTiltakFilter,
  useArrangorTiltaksoversikt,
} from "~/hooks/useArrangorflateTiltaksoversikt";
import { flipObject } from "~/utils/object";
import { useDebounce } from "@mr/frontend-common";

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
            <Suspense fallback={<Laster tekst="Laster tiltak..." size="xlarge" />}>
              <TiltaksOversiktContent
                type={
                  currentTab === "aktive"
                    ? ArrangorflateFilterType.AKTIVE
                    : ArrangorflateFilterType.HISTORISKE
                }
              />
            </Suspense>
          </Tabs.Panel>
        </Tabs>
      </VStack>
    </Box>
  );
}

function TiltaksOversiktContent({ type }: { type: ArrangorflateFilterType }) {
  const [sok, setSok] = useState("");
  const debouncedSok = useDebounce(sok, 300);
  const {
    data: paginertTiltaksRader,
    filter,
    setFilter,
    oppdaterSok,
  } = useArrangorTiltaksoversikt({ type });

  useEffect(() => {
    oppdaterSok(debouncedSok);
  }, [debouncedSok, oppdaterSok]);

  function clearSearch() {
    setSok("");
  }
  const tiltakSortKeyToParam: Record<string, ArrangorflateTiltakFilterOrderBy> = {
    tiltakNavn: ArrangorflateTiltakFilterOrderBy.TILTAK,
    arrangorNavn: ArrangorflateTiltakFilterOrderBy.ARRANGOR,
    startDato: ArrangorflateTiltakFilterOrderBy.START_DATO,
  };

  const paramToSortKey: Record<ArrangorflateTiltakFilterOrderBy, string> =
    flipObject(tiltakSortKeyToParam);

  const paramToSortDirection: Record<ArrangorflateFilterDirection, SortState["direction"]> =
    flipObject({
      ascending: ArrangorflateFilterDirection.ASC,
      descending: ArrangorflateFilterDirection.DESC,
      none: ArrangorflateFilterDirection.ASC,
    });

  function filterToSortState({ orderBy, direction }: ArrangorflateTiltakFilter): SortState {
    const newOrderBy: SortState["orderBy"] = (orderBy && paramToSortKey[orderBy]) || "tiltaksNavn";
    const newDirection: SortState["direction"] =
      (direction && paramToSortDirection[direction]) || "ascending";

    return {
      orderBy: newOrderBy,
      direction: newDirection,
    };
  }

  const paginationProps: PaginationProps | undefined =
    type === ArrangorflateFilterType.HISTORISKE
      ? {
          hidden: !paginertTiltaksRader.pagination.totalPages,
          page: filter.page || 1,
          count: paginertTiltaksRader.pagination.totalPages || 1,
          boundaryCount: 1,
          prevNextTexts: true,
          onPageChange: (newPage) => setFilter((filter) => ({ ...filter, page: newPage })),
        }
      : undefined;

  function sortChange(orderBy: ArrangorflateTiltakFilterOrderBy) {
    if (orderBy == filter.orderBy) {
      const direction =
        filter.direction == ArrangorflateFilterDirection.ASC
          ? ArrangorflateFilterDirection.DESC
          : ArrangorflateFilterDirection.ASC;
      return setFilter((old) => ({ ...old, direction }));
    }

    setFilter((old) => ({
      ...old,
      orderBy,
      direction: ArrangorflateFilterDirection.ASC,
    }));
  }

  if (paginertTiltaksRader.data.length === 0) {
    return (
      <Box marginBlock="space-16">
        <InfoCard data-color="warning" className="my-10">
          <InfoCard.Header>
            <InfoCard.Title>Fant ingen registrerte tiltak</InfoCard.Title>
          </InfoCard.Header>
          <InfoCard.Content>
            <BodyShort spacing>
              Det finnes ingen registrerte tiltak du kan sende inn utbetalingskrav for.
            </BodyShort>
            <BodyShort>Ta eventuelt kontakt med Nav ved behov.</BodyShort>
          </InfoCard.Content>
        </InfoCard>
      </Box>
    );
  }

  return (
    <>
      <Box paddingBlock="space-16" width="30rem">
        <Search
          label="Søk i utbetalinger"
          description="Tiltaksnavn, arrangør, periode, beløp"
          hideLabel={false}
          variant="simple"
          width="30rem"
          onChange={setSok}
          onClear={clearSearch}
        />
      </Box>
      <Tabellvisning
        kolonner={kolonner}
        sort={filterToSortState(filter)}
        onSortChange={(key) => sortChange(tiltakSortKeyToParam[key])}
        pagination={paginationProps}
      >
        {paginertTiltaksRader.data.map((row: ArrangorInnsendingRadDto) => (
          <UtbetalingRow key={row.gjennomforingId} row={row} />
        ))}
      </Tabellvisning>
    </>
  );
}

const kolonner: Array<Kolonne> = [
  { key: "tiltakNavn", label: "Tiltak", sortable: true },
  { key: "arrangorNavn", label: "Arrangør", sortable: true },
  { key: "startDato", label: "Periode", sortable: true },
];
