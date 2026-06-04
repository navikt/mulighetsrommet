import { NavnOgGradering } from "@/components/personalia/NavnOgGradering";
import { useSortableData } from "@mr/frontend-common";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Table } from "@navikt/ds-react";
import { BeregningDeltakerDto, UtbetalingBeregningType } from "@tiltaksadministrasjon/api-client";

interface Props {
  deltakere: BeregningDeltakerDto[];
  type: UtbetalingBeregningType;
}

export function BeregningDeltakereTable({ deltakere, type }: Props) {
  const { sortedData, sort, toggleSort } = useSortableData(deltakere, undefined, (item, key) =>
    key.split(".").reduce((obj: any, k) => obj?.[k], item),
  );

  function faktorName() {
    switch (type) {
      case UtbetalingBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED:
      case UtbetalingBeregningType.PRIS_PER_MANEDSVERK:
        return "Månedsverk";
      case UtbetalingBeregningType.PRIS_PER_UKESVERK:
      case UtbetalingBeregningType.PRIS_PER_HELE_UKESVERK:
        return "Ukesverk";
      case UtbetalingBeregningType.FAST_SATS_PER_AVTALT_TILTAKSPLASS_PER_MANED:
      case UtbetalingBeregningType.PRIS_PER_TIME_OPPFOLGING:
      case UtbetalingBeregningType.FRI:
        return "";
    }
  }

  return (
    <Table sort={sort} onSortChange={(sortKey) => toggleSort(sortKey as string)}>
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeader sortKey="navn" sortable>
            Deltaker
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="region" sortable>
            Region
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="oppfolgingEnhet" sortable>
            Oppfølgingsenhet
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="geografiskEnhet" sortable>
            Geografisk enhet
          </Table.ColumnHeader>
          {type !== UtbetalingBeregningType.PRIS_PER_TIME_OPPFOLGING && (
            <>
              <Table.ColumnHeader sortKey="faktor" sortable>
                {faktorName()}
              </Table.ColumnHeader>
              <Table.ColumnHeader sortKey="belop.belop" sortable>
                Beløp
              </Table.ColumnHeader>
            </>
          )}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((deltaker, i) => {
          return (
            <Table.Row key={i}>
              <Table.HeaderCell scope="row">
                <NavnOgGradering
                  navn={deltaker.navn}
                  gradering={deltaker.gradering}
                  norskIdent={deltaker.norskIdent}
                />
              </Table.HeaderCell>
              <Table.DataCell>{deltaker.region}</Table.DataCell>
              <Table.DataCell>{deltaker.oppfolgingEnhet}</Table.DataCell>
              <Table.DataCell>{deltaker.geografiskEnhet}</Table.DataCell>
              {type !== UtbetalingBeregningType.PRIS_PER_TIME_OPPFOLGING && (
                <>
                  <Table.DataCell>{deltaker.faktor}</Table.DataCell>
                  {deltaker.belop && (
                    <Table.DataCell>{formaterValutaBelop(deltaker.belop)}</Table.DataCell>
                  )}
                </>
              )}
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
