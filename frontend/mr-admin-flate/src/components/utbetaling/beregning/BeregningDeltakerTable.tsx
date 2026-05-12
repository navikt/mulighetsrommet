import { NavnOgGradering } from "@/components/personalia/NavnOgGradering";
import { compare, formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { SortState, Table } from "@navikt/ds-react";
import { BeregningDeltakerDto, UtbetalingBeregningType } from "@tiltaksadministrasjon/api-client";
import { useState } from "react";

interface ScopedSortState extends SortState {
  orderBy: keyof BeregningDeltakerDto;
}

interface Props {
  deltakere: BeregningDeltakerDto[];
  type: UtbetalingBeregningType;
}

export function BeregningDeltakereTable({ deltakere, type }: Props) {
  const [sort, setSort] = useState<ScopedSortState | undefined>();

  function handleSort(sortKey: ScopedSortState["orderBy"]) {
    setSort(
      sort && sortKey === sort.orderBy && sort.direction === "descending"
        ? undefined
        : {
            orderBy: sortKey,
            direction:
              sort && sortKey === sort.orderBy && sort.direction === "ascending"
                ? "descending"
                : "ascending",
          },
    );
  }

  function faktorName() {
    switch (type) {
      case UtbetalingBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED:
      case UtbetalingBeregningType.PRIS_PER_MANEDSVERK:
        return "Månedsverk";
      case UtbetalingBeregningType.PRIS_PER_UKESVERK:
      case UtbetalingBeregningType.PRIS_PER_HELE_UKESVERK:
        return "Ukesverk";
      case UtbetalingBeregningType.PRIS_PER_TIME_OPPFOLGING:
      case UtbetalingBeregningType.FRI:
        return "";
    }
  }

  const sortedData = deltakere.slice().sort((a, b) => {
    if (sort) {
      return sort.direction === "ascending" ? compare(b, a) : compare(a, b);
    }
    return 1;
  });

  return (
    <Table
      sort={sort}
      onSortChange={(sortKey) => handleSort(sortKey as ScopedSortState["orderBy"])}
    >
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
              <Table.ColumnHeader sortKey="belop" sortable>
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
