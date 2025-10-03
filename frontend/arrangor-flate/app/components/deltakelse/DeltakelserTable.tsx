import {
  formaterDato,
  formaterPeriodeSlutt,
  formaterPeriodeStart,
} from "@mr/frontend-common/utils/date";
import { ExclamationmarkTriangleIcon } from "@navikt/aksel-icons";
import { Alert, HStack, List, Table, Tooltip } from "@navikt/ds-react";
import {
  ArrangorflateBeregning,
  ArrangorflateBeregningDeltakelse,
  ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed,
  ArrangorflateBeregningDeltakelsePrisPerManedsverk,
  ArrangorflateBeregningDeltakelsePrisPerUkesverk,
  DeltakerAdvarsel,
  Periode,
} from "api-client";
import { useSortState } from "~/hooks/useSortState";
import { tekster } from "~/tekster";
import { sortBy, SortBySelector } from "~/utils/sort-by";
import { DeltakelseTimeline } from "./DeltakelseTimeline";

enum DeltakerSortKey {
  PERSON_NAVN = "PERSON_NAVN",
  PERIODE_START = "PERIODE_START",
  PERIODE_SLUTT = "PERIODE_SLUTT",
}

function getDeltakerSelector(
  sortKey: DeltakerSortKey,
): SortBySelector<ArrangorflateBeregningDeltakelse> {
  switch (sortKey) {
    case DeltakerSortKey.PERSON_NAVN:
      return (d) => d.personalia?.navn;
    case DeltakerSortKey.PERIODE_START:
      return (d) => d.periode.start;
    case DeltakerSortKey.PERIODE_SLUTT:
      return (d) => d.periode.slutt;
  }
}

interface Column<T> {
  label: string;
  sortable?: boolean;
  sortKey?: DeltakerSortKey;
  headerCell?: boolean;
  align?: "left" | "right";
  render: (d: T, hasRelevanteForslag: boolean) => React.ReactNode;
}

const baseColumns: Column<ArrangorflateBeregningDeltakelse>[] = [
  {
    label: "Navn",
    sortable: true,
    sortKey: DeltakerSortKey.PERSON_NAVN,
    headerCell: true,
    render: (d, hasRelevanteForslag) => (
      <HStack gap="2">
        {hasRelevanteForslag && (
          <Tooltip content="Har ubehandlede forslag som påvirker utbetalingen">
            <ExclamationmarkTriangleIcon fontSize="1.5rem" />
          </Tooltip>
        )}
        {d.personalia?.navn ?? "-"}
      </HStack>
    ),
  },
  { label: "Ident", render: (d) => d.personalia?.norskIdent },
  { label: "Startdato i tiltaket", render: (d) => formaterDato(d.deltakerStartDato) ?? "-" },
  {
    label: "Startdato i perioden",
    sortable: true,
    sortKey: DeltakerSortKey.PERIODE_START,
    render: (d) => formaterPeriodeStart(d.periode),
  },
  {
    label: "Sluttdato i perioden",
    sortable: true,
    sortKey: DeltakerSortKey.PERIODE_SLUTT,
    render: (d) => formaterPeriodeSlutt(d.periode),
  },
];

type DeltakerTypeMap = {
  ArrangorflateBeregningPrisPerManedsverk: ArrangorflateBeregningDeltakelsePrisPerManedsverk;
  ArrangorflateBeregningFastSatsPerTiltaksplassPerManed: ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed;
  ArrangorflateBeregningPrisPerUkesverk: ArrangorflateBeregningDeltakelsePrisPerUkesverk;
};

const columns: {
  [K in keyof DeltakerTypeMap]: Column<DeltakerTypeMap[K]>[];
} = {
  ArrangorflateBeregningFastSatsPerTiltaksplassPerManed: [
    ...baseColumns,
    {
      label: "Deltakelsesprosent",
      align: "right",
      render: (d) => d.perioderMedDeltakelsesmengde.at(-1)?.deltakelsesprosent,
    },
    { label: "Månedsverk", align: "right", render: (d) => d.faktor },
    { label: "", render: () => null },
  ],
  ArrangorflateBeregningPrisPerManedsverk: [
    ...baseColumns,
    { label: "Månedsverk", align: "right", render: (d) => d.faktor },
    { label: "", render: () => null },
  ],
  ArrangorflateBeregningPrisPerUkesverk: [
    ...baseColumns,
    { label: "Ukesverk", align: "right", render: (d) => d.faktor },
    { label: "", render: () => null },
  ],
};

export function DeltakelserTable({
  periode,
  beregning,
  advarsler,
}: {
  periode: Periode;
  beregning: ArrangorflateBeregning;
  advarsler: DeltakerAdvarsel[];
  deltakerlisteUrl: string;
}) {
  const { sort, handleSort } = useSortState<DeltakerSortKey>();

  if (!beregning.type || !("deltakelser" in beregning)) {
    return null;
  }

  const sortedData = sort
    ? sortBy(beregning.deltakelser, sort.direction, getDeltakerSelector(sort.orderBy))
    : beregning.deltakelser;

  function hasAdvarsel(id: string): boolean {
    return advarsler.some((r) => r.deltakerId === id);
  }

  const cols = columns[beregning.type] as Column<ArrangorflateBeregningDeltakelse>[];

  return (
    <>
      {advarsler.length > 0 && (
        <Alert variant="warning">
          {tekster.bokmal.utbetaling.beregning.advarslerFinnes}
          <List>
            {advarsler.map((advarsel) => (
              <List.Item key={advarsel.deltakerId}>
                <DeltakerAdvarselInfo
                  advarsel={advarsel}
                  deltaker={beregning.deltakelser.find((d) => d.id === advarsel.deltakerId)}
                />
              </List.Item>
            ))}
          </List>
        </Alert>
      )}
      <Table sort={sort} onSortChange={(sortKey) => handleSort(sortKey as DeltakerSortKey)}>
        <Table.Header>
          <Table.Row>
            {cols.map((col) => (
              <Table.ColumnHeader
                key={col.label}
                scope="col"
                sortable={col.sortable}
                sortKey={col.sortKey}
              >
                {col.label}
              </Table.ColumnHeader>
            ))}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {sortedData.map((deltakelse, index) => {
            return (
              <Table.ExpandableRow
                key={deltakelse.id}
                content={
                  <DeltakelseTimeline
                    utbetalingsperiode={periode}
                    stengt={beregning.stengt}
                    deltakelse={deltakelse}
                  />
                }
                togglePlacement="right"
                className={
                  hasAdvarsel(deltakelse.id)
                    ? "bg-surface-warning-moderate"
                    : index % 2 !== 0
                      ? "bg-surface-subtle"
                      : "" // zebra stripes gjøres her fordi den overskriver warning background
                }
              >
                {cols.map((col) => {
                  const children = col.render(deltakelse, hasAdvarsel(deltakelse.id));
                  if (children === null) {
                    return null;
                  }

                  return col.headerCell ? (
                    <Table.HeaderCell key={col.label}>{children}</Table.HeaderCell>
                  ) : (
                    <Table.DataCell key={col.label}>{children}</Table.DataCell>
                  );
                })}
              </Table.ExpandableRow>
            );
          })}
        </Table.Body>
      </Table>
    </>
  );
}

function DeltakerAdvarselInfo({
  deltaker,
  advarsel,
}: {
  deltaker?: ArrangorflateBeregningDeltakelse;
  advarsel: DeltakerAdvarsel;
}) {
  switch (advarsel.type) {
    case "DeltakerAdvarselRelevanteForslag":
      return `${deltaker?.personalia?.navn} har ubehandlede forslag. Disse må først godkjennes av Nav-veileder før utbetalingen oppdaterer seg`;
    case "DeltakerAdvarselFeilSluttDato":
      return `${deltaker?.personalia?.navn} har status “${deltaker?.status}” og slutt dato frem i tid`;
    case "DeltakerAdvarselOverlappendePeriode":
      return `${deltaker?.personalia?.navn} har flere deltakelser med overlappende perioder`;
    case undefined:
      throw new Error('"type" mangler fra advarsel');
  }
}
