import { opplaeringTilskuddToString, tilskuddMottakerToString } from "@/utils/Utils";
import {
  KostnadsstedDto,
  TilskuddUtbetalingKompaktDto,
  UtbetalingStatusDto,
} from "@tiltaksadministrasjon/api-client";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Table, Link } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useMemo } from "react";
import { Link as ReactRouterLink } from "react-router";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { DataElementStatusTag, useSortableData } from "@mr/frontend-common";
import { formaterPeriodeSlutt, formaterPeriodeStart } from "@mr/frontend-common/utils/date";

interface Props {
  gjennomforingId: string;
  utbetalinger: TilskuddUtbetalingKompaktDto[];
}

interface UtbetalingRow {
  periodeStart: string;
  periodeSlutt: string;
  status: UtbetalingStatusDto;
  kostnadssteder: KostnadsstedDto[];
}

export function TilskuddUtbetalingTable({ gjennomforingId, utbetalinger }: Props) {
  const { sortedData, sort, toggleSort } = useSortableData(
    useMemo(() => {
      return utbetalinger.map((u) => ({
        ...u,
        periodeStart: u.periode.start,
        periodeSlutt: u.periode.slutt,
      }));
    }, [utbetalinger]),
  );

  return (
    <Table
      sort={sort}
      onSortChange={(sortKey) => toggleSort(sortKey as keyof UtbetalingRow)}
      aria-label="Utbetalinger"
      data-testid="utbetaling-table"
    >
      <Table.Header>
        <Table.Row>
          <TableColumnHeader sortKey="periodeStart" sortable>
            {utbetalingTekster.periode.start.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="periodeSlutt" sortable>
            {utbetalingTekster.periode.slutt.label}
          </TableColumnHeader>
          <Table.ColumnHeader sortKey="type" sortable>
            Type
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="vedtakResultat" sortable>
            Vedtakresultat
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="kostnadssted" sortable>
            Kostnadssted
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="mottaker" sortable>
            Mottaker
          </Table.ColumnHeader>
          <TableColumnHeader sortKey="belopUtbetalt" sortable align="right">
            {utbetalingTekster.beregning.belop.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="status" sortable>
            Status
          </TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map(
          ({
            belopUtbetalt,
            periode,
            id,
            status,
            kostnadssted,
            type,
            vedtakResultat,
            mottaker,
            tilskuddBehandlingId,
          }) => {
            return (
              <Table.Row key={id}>
                <Table.DataCell>{formaterPeriodeStart(periode)}</Table.DataCell>
                <Table.DataCell>{formaterPeriodeSlutt(periode)}</Table.DataCell>
                <Table.DataCell>{opplaeringTilskuddToString(type)}</Table.DataCell>
                <Table.DataCell>
                  {<DataElementStatusTag {...vedtakResultat.status} />}
                </Table.DataCell>
                <Table.DataCell> {kostnadssted?.navn ?? "-"} </Table.DataCell>
                <Table.DataCell>{tilskuddMottakerToString(mottaker)}</Table.DataCell>
                <Table.DataCell align="right">
                  {belopUtbetalt ? formaterValutaBelop(belopUtbetalt) : ""}
                </Table.DataCell>
                <Table.DataCell width="min-content">
                  <DataElementStatusTag
                    value={status.status.value}
                    variant={status.status.variant}
                  />
                </Table.DataCell>
                <Table.DataCell>
                  <Link
                    as={ReactRouterLink}
                    to={`/gjennomforinger/${gjennomforingId}/tilskudd-behandling/${tilskuddBehandlingId}`}
                  >
                    Detaljer
                  </Link>
                </Table.DataCell>
              </Table.Row>
            );
          },
        )}
      </Table.Body>
    </Table>
  );
}
