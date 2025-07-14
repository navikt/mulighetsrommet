import { ExclamationmarkTriangleIcon } from "@navikt/aksel-icons";
import { Alert, HStack, List, Table, Tooltip } from "@navikt/ds-react";
import { useSortState } from "~/hooks/useSortState";
import { formaterFoedselsdato } from "~/utils/date";
import { sortBy, SortBySelector } from "~/utils/sort-by";
import { DeltakelseTimeline } from "./DeltakelseTimeline";
import {
  ArrFlateUtbetalingDeltakelse,
  ArrFlateBeregningPrisPerManedsverk,
  ArrFlateBeregningPrisPerManedsverkMedDeltakelsesmengder,
  ArrFlateBeregningPrisPerUkesverk,
  Periode,
  RelevanteForslag,
} from "api-client";
import { tekster } from "~/tekster";
import { formaterDato } from "@mr/frontend-common/utils/date";

enum DeltakerSortKey {
  PERSON_NAVN = "PERSON_NAVN",
  PERIODE_START = "PERIODE_START",
  PERIODE_SLUTT = "PERIODE_SLUTT",
}

function getDeltakerSelector(
  sortKey: DeltakerSortKey,
): SortBySelector<ArrFlateUtbetalingDeltakelse> {
  switch (sortKey) {
    case DeltakerSortKey.PERSON_NAVN:
      return (d) => d.person?.navn;
    case DeltakerSortKey.PERIODE_START:
      return (d) => d.periodeStartDato;
    case DeltakerSortKey.PERIODE_SLUTT:
      return (d) => d.periodeSluttDato;
  }
}

export function DeltakelserTable({
  periode,
  beregning,
  relevanteForslag,
}: {
  periode: Periode;
  beregning:
    | ArrFlateBeregningPrisPerManedsverkMedDeltakelsesmengder
    | ArrFlateBeregningPrisPerManedsverk
    | ArrFlateBeregningPrisPerUkesverk;
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
    (deltaker: ArrFlateUtbetalingDeltakelse) => hasRelevanteForslag(deltaker.id),
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
            <Table.ColumnHeader scope="col" sortable sortKey={DeltakerSortKey.PERSON_NAVN}>
              Navn
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fødselsdato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Startdato i tiltaket</Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortable sortKey={DeltakerSortKey.PERIODE_START}>
              Startdato i perioden
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col" sortable sortKey={DeltakerSortKey.PERIODE_SLUTT}>
              Sluttdato i perioden
            </Table.ColumnHeader>
            <Table.ColumnHeader align="right" scope="col">
              Deltakelsesprosent
            </Table.ColumnHeader>
            <Table.ColumnHeader align="right" scope="col">
              {tekster.bokmal.utbetaling.beregning.deltakelsesfaktor(beregning.type)}
            </Table.ColumnHeader>
            <Table.ColumnHeader scope="col"></Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {sortedData.map((deltakelse, index) => {
            const { id, person } = deltakelse;
            const fodselsdato = formaterFoedselsdato(person?.fodselsdato, person?.fodselsaar);
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
                <Table.HeaderCell scope="row">
                  <HStack gap="2">
                    {hasRelevanteForslag(id) && (
                      <Tooltip content="Har ubehandlede forslag som påvirker utbetalingen">
                        <ExclamationmarkTriangleIcon fontSize="1.5rem" />
                      </Tooltip>
                    )}
                    {person?.navn}
                  </HStack>
                </Table.HeaderCell>
                <Table.DataCell>{fodselsdato}</Table.DataCell>
                <Table.DataCell>
                  {deltakelse.deltakerStartDato ? formaterDato(deltakelse.deltakerStartDato) : "-"}
                </Table.DataCell>
                <Table.DataCell>{formaterDato(deltakelse.periodeStartDato)}</Table.DataCell>
                <Table.DataCell>{formaterDato(deltakelse.periodeSluttDato)}</Table.DataCell>
                <Table.DataCell align="right">
                  {deltakelse.perioderMedDeltakelsesmengde.at(-1)?.deltakelsesprosent}
                </Table.DataCell>
                <Table.DataCell align="right">{deltakelse.faktor}</Table.DataCell>
              </Table.ExpandableRow>
            );
          })}
        </Table.Body>
      </Table>
    </>
  );
}
