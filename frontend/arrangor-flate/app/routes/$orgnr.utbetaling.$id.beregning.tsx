import {
  ArrangorflateService,
  ArrFlateBeregningForhandsgodkjent,
  ArrFlateUtbetaling,
  Periode,
  RelevanteForslag,
  UtbetalingDeltakelse,
  UtbetalingDeltakelsePerson,
  UtbetalingStengtPeriode,
} from "api-client";
import { ExclamationmarkTriangleIcon, ParasolBeachIcon, PersonIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Button,
  GuidePanel,
  HGrid,
  HStack,
  List,
  SortState,
  Table,
  Timeline,
  Tooltip,
  VStack,
} from "@navikt/ds-react";
import { useState } from "react";
import type { LoaderFunction, MetaFunction } from "react-router";
import { Link, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { LinkWithTabState } from "~/components/LinkWithTabState";
import { PageHeader } from "~/components/PageHeader";
import { GenerelleDetaljer } from "~/components/utbetaling/GenerelleDetaljer";
import { internalNavigation } from "~/internal-navigation";
import { hentMiljø, Miljø } from "~/services/miljø";
import {
  formaterDato,
  formaterPeriode,
  problemDetailResponse,
  subtractDays,
  useOrgnrFromUrl,
} from "~/utils";
import { sortBy, SortBySelector, SortOrder } from "~/utils/sort-by";
import { BeregningDetaljer } from "../components/utbetaling/BeregningDetaljer";

export const meta: MetaFunction = () => {
  return [
    { title: "Beregning" },
    { name: "description", content: "Beregning for krav om utbetaling" },
  ];
};

type LoaderData = {
  utbetaling: ArrFlateUtbetaling;
  relevanteForslag: RelevanteForslag[];
  deltakerlisteUrl: string;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const deltakerlisteUrl = deltakerOversiktLenke(hentMiljø());

  const { id } = params;
  if (!id) {
    throw new Response("Mangler id", { status: 400 });
  }

  const [
    { data: utbetaling, error: utbetalingError },
    { data: relevanteForslag, error: relevanteForslagError },
  ] = await Promise.all([
    ArrangorflateService.getArrFlateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getRelevanteForslag({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError || !utbetaling) {
    throw problemDetailResponse(utbetalingError);
  }
  if (relevanteForslagError || !relevanteForslag) {
    throw problemDetailResponse(relevanteForslagError);
  }

  return { utbetaling, deltakerlisteUrl, relevanteForslag };
};

interface DeltakerSortState extends SortState {
  direction: SortOrder;
  orderBy: DeltakerSortKey;
}

enum DeltakerSortKey {
  PERSON_NAVN = "PERSON_NAVN",
  PERIODE_START = "PERIODE_START",
  PERIODE_SLUTT = "PERIODE_SLUTT",
  VEILEDER_NAVN = "VEILEDER_NAVN",
}

export default function UtbetalingBeregning() {
  const orgnr = useOrgnrFromUrl();
  const { utbetaling, deltakerlisteUrl, relevanteForslag } = useLoaderData<LoaderData>();

  let beregning = null;
  if (utbetaling.beregning.type === "FORHANDSGODKJENT") {
    beregning = (
      <ForhandsgodkjentBeregning
        periode={utbetaling.periode}
        beregning={utbetaling.beregning}
        relevanteForslag={relevanteForslag}
        deltakerlisteUrl={deltakerlisteUrl}
      />
    );
  }

  return (
    <>
      <PageHeader
        title="Beregning"
        tilbakeLenke={{
          navn: "Tilbake til utbetalinger",
          url: internalNavigation(orgnr).utbetalinger,
        }}
      />
      <HGrid gap="5" columns={1}>
        <GenerelleDetaljer className="max-w-[50%]" utbetaling={utbetaling} />
      </HGrid>
      <div className="mt-4">{beregning}</div>
      <VStack align="end">
        <BeregningDetaljer beregning={utbetaling.beregning} />
      </VStack>
      <HStack justify="end" className="mt-4">
        <Button
          as={LinkWithTabState}
          className="justify-self-end"
          to={internalNavigation(orgnr).bekreft(utbetaling.id)}
        >
          Neste
        </Button>
      </HStack>
    </>
  );
}

function ForhandsgodkjentBeregning({
  periode,
  beregning,
  relevanteForslag,
  deltakerlisteUrl,
}: {
  periode: Periode;
  beregning: ArrFlateBeregningForhandsgodkjent;
  relevanteForslag: RelevanteForslag[];
  deltakerlisteUrl: string;
}) {
  const [sort, setSort] = useState<DeltakerSortState | undefined>();

  const handleSort = (orderBy: string) => {
    if (!isDeltakerSortKey(orderBy)) {
      return;
    }

    if (sort && orderBy === sort.orderBy && sort.direction === "descending") {
      setSort(undefined);
    } else {
      const direction =
        sort && orderBy === sort.orderBy && sort.direction === "ascending"
          ? "descending"
          : "ascending";
      setSort({ orderBy, direction });
    }
  };

  const sortedData = sort
    ? sortBy(beregning.deltakelser, sort.direction, getDeltakerSelector(sort.orderBy))
    : beregning.deltakelser;

  function hasRelevanteForslag(id: string): boolean {
    return (relevanteForslag.find((r) => r.deltakerId === id)?.antallRelevanteForslag ?? 0) > 0;
  }

  const deltakereMedRelevanteForslag = sortedData.filter((deltaker: UtbetalingDeltakelse) =>
    hasRelevanteForslag(deltaker.id),
  );

  return (
    <>
      <HGrid gap="5" columns={1}>
        <GuidePanel>
          Hvis noen av opplysningene om deltakerne ikke stemmer, må det sendes forslag til Nav om
          endring via{" "}
          <Link className="underline" to={deltakerlisteUrl}>
            Deltakeroversikten
          </Link>
          .
        </GuidePanel>
        {beregning.stengt.length > 0 && (
          <Alert variant={"info"}>
            Det er registrert stengt hos arrangør i følgende perioder:
            <ul>
              {beregning.stengt.map(({ periode, beskrivelse }) => (
                <li key={periode.start + periode.slutt}>
                  {formaterPeriode(periode)}: {beskrivelse}
                </li>
              ))}
            </ul>
          </Alert>
        )}
        {deltakereMedRelevanteForslag.length > 0 && (
          <Alert variant="warning">
            Det finnes ubehandlede forslag som påvirker utbetalinger på følgende personer. Disse må
            først godkjennes av Nav-veileder før utbetalingen oppdaterer seg.
            <List>
              {deltakereMedRelevanteForslag.map((deltaker) => (
                <List.Item key={deltaker.id}>{deltaker.person?.navn}</List.Item>
              ))}
            </List>
          </Alert>
        )}
        <Table sort={sort} onSortChange={(sortKey) => handleSort(sortKey)}>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader scope="col" sortable sortKey={DeltakerSortKey.PERSON_NAVN}>
                Navn
              </Table.ColumnHeader>
              <Table.ColumnHeader align="right" scope="col">
                Fødselsdato
              </Table.ColumnHeader>
              <Table.ColumnHeader align="right" scope="col">
                Startdato i tiltaket
              </Table.ColumnHeader>
              <Table.ColumnHeader
                align="right"
                scope="col"
                sortable
                sortKey={DeltakerSortKey.PERIODE_START}
              >
                Startdato i perioden
              </Table.ColumnHeader>
              <Table.ColumnHeader
                align="right"
                scope="col"
                sortable
                sortKey={DeltakerSortKey.PERIODE_SLUTT}
              >
                Sluttdato i perioden
              </Table.ColumnHeader>
              <Table.ColumnHeader align="right" scope="col">
                Deltakelsesprosent
              </Table.ColumnHeader>
              <Table.ColumnHeader align="right" scope="col">
                Månedsverk i perioden
              </Table.ColumnHeader>

              <Table.HeaderCell scope="col"></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {sortedData.map((deltakelse, index) => {
              const { id, person } = deltakelse;
              const fodselsdato = getFormattedFodselsdato(person);
              return (
                <Table.ExpandableRow
                  key={id}
                  content={
                    <DeltakelseTimeline
                      utbetalingsperiode={periode}
                      stengt={beregning.stengt}
                      deltakelse={deltakelse}
                    />
                  }
                  togglePlacement="right"
                  className={
                    hasRelevanteForslag(id)
                      ? "bg-surface-warning-moderate"
                      : index % 2 !== 0
                        ? "bg-surface-subtle"
                        : "" // zebra stripes gjøres her fordi den overskriver warning background
                  }
                >
                  <Table.DataCell className="font-bold">
                    {hasRelevanteForslag(id) && (
                      <Tooltip content="Har ubehandlede forslag som påvirker utbetalingen">
                        <ExclamationmarkTriangleIcon fontSize="1.5rem" />
                      </Tooltip>
                    )}
                    {person?.navn}
                  </Table.DataCell>
                  <Table.DataCell align="right">{fodselsdato}</Table.DataCell>
                  <Table.DataCell align="right">
                    {formaterDato(deltakelse.startDato)}
                  </Table.DataCell>
                  <Table.DataCell align="right">
                    {formaterDato(deltakelse.forstePeriodeStartDato)}
                  </Table.DataCell>
                  <Table.DataCell align="right">
                    {formaterDato(deltakelse.sistePeriodeSluttDato)}
                  </Table.DataCell>
                  <Table.DataCell align="right">
                    {deltakelse.sistePeriodeDeltakelsesprosent}
                  </Table.DataCell>
                  <Table.DataCell align="right">{deltakelse.manedsverk}</Table.DataCell>
                </Table.ExpandableRow>
              );
            })}
          </Table.Body>
        </Table>
      </HGrid>
    </>
  );
}

function isDeltakerSortKey(sortKey: string): sortKey is DeltakerSortKey {
  return sortKey in DeltakerSortKey;
}

function getDeltakerSelector(sortKey: DeltakerSortKey): SortBySelector<UtbetalingDeltakelse> {
  switch (sortKey) {
    case DeltakerSortKey.PERSON_NAVN:
      return (d) => d.person?.navn;
    case DeltakerSortKey.PERIODE_START:
      return (d) => d.forstePeriodeStartDato;
    case DeltakerSortKey.PERIODE_SLUTT:
      return (d) => d.sistePeriodeSluttDato;
    case DeltakerSortKey.VEILEDER_NAVN:
      return (d) => d.veileder;
  }
}

function getFormattedFodselsdato(person?: UtbetalingDeltakelsePerson) {
  return person?.fodselsdato
    ? formaterDato(person.fodselsdato)
    : person?.fodselsaar
      ? `Fødselsår: ${person.fodselsaar}`
      : null;
}

function deltakerOversiktLenke(miljo: Miljø): string {
  if (miljo === Miljø.DevGcp) {
    return "https://amt.intern.dev.nav.no/deltakeroversikt";
  }
  return "https://nav.no/deltakeroversikt";
}

interface DeltakelseTimelineProps {
  utbetalingsperiode: Periode;
  stengt: UtbetalingStengtPeriode[];
  deltakelse: UtbetalingDeltakelse;
}

function DeltakelseTimeline({ utbetalingsperiode, stengt, deltakelse }: DeltakelseTimelineProps) {
  return (
    <Timeline
      startDate={new Date(utbetalingsperiode.start)}
      endDate={new Date(utbetalingsperiode.slutt)}
    >
      <Timeline.Row label="Deltakelse" icon={<PersonIcon aria-hidden />}>
        {deltakelse.perioder.map(({ periode, deltakelsesprosent }) => {
          const start = new Date(periode.start);
          const end = subtractDays(new Date(periode.slutt), 1);
          const label = `${formaterPeriode(periode)}: Deltakelse på ${deltakelsesprosent}%`;
          return (
            <Timeline.Period
              key={periode.start + periode.slutt}
              start={start}
              end={end}
              status={"success"}
              icon={<PersonIcon aria-hidden />}
              statusLabel={label}
            >
              {label}
            </Timeline.Period>
          );
        })}
      </Timeline.Row>
      <Timeline.Row label="Stengt" icon={<ParasolBeachIcon aria-hidden />}>
        {stengt.map(({ periode, beskrivelse }) => {
          const start = new Date(periode.start);
          const end = subtractDays(new Date(periode.slutt), 1);
          const label = `${formaterPeriode(periode)}: ${beskrivelse}`;
          return (
            <Timeline.Period
              key={periode.start + periode.slutt}
              start={start}
              end={end}
              status={"info"}
              icon={<ParasolBeachIcon aria-hidden />}
              statusLabel={label}
            >
              {label}
            </Timeline.Period>
          );
        })}
      </Timeline.Row>
    </Timeline>
  );
}
