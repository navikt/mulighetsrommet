import { ExclamationmarkTriangleIcon } from "@navikt/aksel-icons";
import { Alert, HStack, List, Table, Tooltip } from "@navikt/ds-react";
import { useSortState } from "~/hooks/useSortState";
import { sortBy, SortBySelector } from "~/utils/sort-by";
import { DeltakelseTimeline } from "./DeltakelseTimeline";
import {
  ArrFlateBeregning,
  ArrFlateBeregningDeltakelse,
  ArrFlateBeregningFriDeltakelse,
  ArrFlateBeregningPrisPerManedsverkDeltakelse,
  ArrFlateBeregningPrisPerManedsverkMedDeltakelsesmengderDeltakelse,
  ArrFlateBeregningPrisPerUkesverkDeltakelse,
  Periode,
  RelevanteForslag,
} from "api-client";
import { tekster } from "~/tekster";
import {
  formaterDato,
  formaterPeriodeSlutt,
  formaterPeriodeStart,
} from "@mr/frontend-common/utils/date";

enum DeltakerSortKey {
  PERSON_NAVN = "PERSON_NAVN",
  PERIODE_START = "PERIODE_START",
  PERIODE_SLUTT = "PERIODE_SLUTT",
}

function getDeltakerSelector(
  sortKey: DeltakerSortKey,
): SortBySelector<ArrFlateBeregningDeltakelse> {
  switch (sortKey) {
    case DeltakerSortKey.PERSON_NAVN:
      return (d) => d.person?.navn;
    case DeltakerSortKey.PERIODE_START:
      return (d) => d.periode.start;
    case DeltakerSortKey.PERIODE_SLUTT:
      return (d) => d.periode.slutt;
  }
}

interface Column<T> {
  label: string;
  key: string;
  sortable?: boolean;
  sortKey?: DeltakerSortKey;
  headerCell?: boolean;
  align?: "left" | "right";
  render: (d: T, hasRelevanteForslag: boolean) => React.ReactNode;
}

const baseColumns: Column<ArrFlateBeregningDeltakelse>[] = [
  {
    key: "1",
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
        {d.person?.navn}
      </HStack>
    ),
  },
  { key: "2", label: "Fødselsdato", render: (d) => formaterDato(d.person?.foedselsdato, "-") },
  {
    key: "3",
    label: "Startdato i tiltaket",
    render: (d) => formaterDato(d.deltakerStartDato, "-"),
  },
  {
    key: "4",
    label: "Startdato i perioden",
    sortable: true,
    sortKey: DeltakerSortKey.PERIODE_START,
    render: (d) => formaterPeriodeStart(d.periode),
  },
  {
    key: "5",
    label: "Sluttdato i perioden",
    sortable: true,
    sortKey: DeltakerSortKey.PERIODE_SLUTT,
    render: (d) => formaterPeriodeSlutt(d.periode),
  },
];

type DeltakerTypeMap = {
  PRIS_PER_MANEDSVERK_MED_DELTAKELSESMENGDER: ArrFlateBeregningPrisPerManedsverkMedDeltakelsesmengderDeltakelse;
  PRIS_PER_MANEDSVERK: ArrFlateBeregningPrisPerManedsverkDeltakelse;
  PRIS_PER_UKESVERK: ArrFlateBeregningPrisPerUkesverkDeltakelse;
  FRI: ArrFlateBeregningFriDeltakelse;
};

const columns: {
  [K in keyof DeltakerTypeMap]: Column<DeltakerTypeMap[K]>[];
} = {
  PRIS_PER_MANEDSVERK_MED_DELTAKELSESMENGDER: [
    ...baseColumns,
    {
      key: "6",
      label: "Deltakelsesprosent",
      align: "right",
      render: (d) => d.perioderMedDeltakelsesmengde.at(-1)?.deltakelsesprosent,
    },
    { key: "7", label: "Månedsverk", align: "right", render: (d) => d.faktor },
    { key: "8", label: "", render: () => "" },
  ],
  PRIS_PER_MANEDSVERK: [
    ...baseColumns,
    { key: "7", label: "Månedsverk", align: "right", render: (d) => d.faktor },
    { key: "8", label: "", render: () => "" },
  ],
  PRIS_PER_UKESVERK: [
    ...baseColumns,
    { key: "7", label: "Månedsverk", align: "right", render: (d) => d.faktor },
    { key: "8", label: "", render: () => "" },
  ],
  FRI: [...baseColumns, { key: "8", label: "", render: () => "" }],
};

export function DeltakelserTable({
  periode,
  beregning,
  relevanteForslag,
}: {
  periode: Periode;
  beregning: ArrFlateBeregning;
  relevanteForslag: RelevanteForslag[];
  deltakerlisteUrl: string;
}) {
  const { sort, handleSort } = useSortState<DeltakerSortKey>();

  const sortedData = sort
    ? sortBy(beregning.deltakelser, sort.direction, getDeltakerSelector(sort.orderBy))
    : beregning.deltakelser;

  function hasRelevanteForslag(id: string): boolean {
    return (relevanteForslag.find((r) => r.deltakerId === id)?.antallRelevanteForslag ?? 0) > 0;
  }

  const deltakereMedRelevanteForslag = beregning.deltakelser.filter(
    (deltaker: ArrFlateBeregningDeltakelse) => hasRelevanteForslag(deltaker.id),
  );

  return (
    <>
      {deltakereMedRelevanteForslag.length > 0 && (
        <Alert variant="warning">
          {tekster.bokmal.utbetaling.beregning.ubehandledeDeltakerforslag}
          <List>
            {deltakereMedRelevanteForslag.map((deltaker) => (
              <List.Item key={deltaker.id}>{deltaker.person?.navn}</List.Item>
            ))}
          </List>
        </Alert>
      )}
      <Table sort={sort} onSortChange={(sortKey) => handleSort(sortKey as DeltakerSortKey)}>
        <Table.Header>
          <Table.Row>
            {columns[beregning.type].map((col) => (
              <Table.ColumnHeader
                key={col.key}
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
            const cols = columns[beregning.type] as Column<typeof deltakelse>[];

            return (
              <Table.ExpandableRow
                key={deltakelse.id}
                content={
                  beregning.type !== "FRI" && (
                    <DeltakelseTimeline
                      utbetalingsperiode={periode}
                      stengt={beregning.stengt}
                      deltakelse={deltakelse}
                    />
                  )
                }
                expansionDisabled={beregning.type === "FRI"}
                togglePlacement="right"
                className={
                  hasRelevanteForslag(deltakelse.id)
                    ? "bg-surface-warning-moderate"
                    : index % 2 !== 0
                      ? "bg-surface-subtle"
                      : "" // zebra stripes gjøres her fordi den overskriver warning background
                }
              >
                {cols.map((col) =>
                  col.headerCell ? (
                    <Table.HeaderCell>
                      {col.render(deltakelse, hasRelevanteForslag(deltakelse.id))}
                    </Table.HeaderCell>
                  ) : (
                    <Table.DataCell>
                      {col.render(deltakelse, hasRelevanteForslag(deltakelse.id))}
                    </Table.DataCell>
                  ),
                )}
              </Table.ExpandableRow>
            );
          })}
        </Table.Body>
      </Table>
    </>
  );
}
