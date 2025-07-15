import { isBeregningPrisPerManedsverk } from "@/pages/gjennomforing/tilsagn/tilsagnUtils";
import { formaterPeriodeSlutt, formaterPeriodeStart } from "@/utils/Utils";
import { TilsagnDto, TilsagnStatus } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Table } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { Link, useParams } from "react-router";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { useSortableData } from "@mr/frontend-common";
import { useMemo } from "react";
import { TilsagnTag } from "@/components/tilsagn/TilsagnTag";

interface TilsagnRow {
  periodeStart: string;
  periodeSlutt: string;
  navnForKostnadssted: string;
  antallPlasser: number | null;
  belop: number;
}

interface Props {
  tilsagn: TilsagnDto[];
}

export function TilsagnTabell({ tilsagn }: Props) {
  const { gjennomforingId } = useParams();

  const { sortedData, sort, toggleSort } = useSortableData(
    useMemo(() => {
      return tilsagn.map((t) => ({
        ...t,
        periodeStart: t.periode.start,
        periodeSlutt: t.periode.slutt,
        type: t.type,
        navnForKostnadssted: t.kostnadssted.navn,
        antallPlasser: getAntallPlasser(t),
        belop: t.beregning.belop,
        status: t.status,
      }));
    }, [tilsagn]),
  );

  return (
    <Table sort={sort} onSortChange={(sortKey) => toggleSort(sortKey as keyof TilsagnRow)}>
      <Table.Header>
        <Table.Row>
          <TableColumnHeader sortKey="bestillingsnummer" sortable>
            {tilsagnTekster.bestillingsnummer.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="periodeStart" sortable>
            {tilsagnTekster.periode.start.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="periodeSlutt" sortable>
            {tilsagnTekster.periode.slutt.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="type" sortable>
            {tilsagnTekster.type.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="navnForKostnadssted" sortable>
            {tilsagnTekster.kostnadssted.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="antallPlasser" sortable align="right">
            {tilsagnTekster.antallPlasser.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="belop" sortable align="right">
            {tilsagnTekster.beregning.belop.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="status" sortable align="right">
            {tilsagnTekster.status.label}
          </TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((tilsagn) => {
          const { periode, kostnadssted, beregning, id } = tilsagn;
          return (
            <Table.Row key={id}>
              <Table.DataCell>{tilsagn.bestillingsnummer}</Table.DataCell>
              <Table.DataCell>{formaterPeriodeStart(periode)}</Table.DataCell>
              <Table.DataCell>{formaterPeriodeSlutt(periode)}</Table.DataCell>
              <Table.DataCell>{avtaletekster.tilsagn.type(tilsagn.type)}</Table.DataCell>
              <Table.DataCell>{kostnadssted.navn}</Table.DataCell>
              <Table.DataCell align="right">{getAntallPlasser(tilsagn)}</Table.DataCell>
              <Table.DataCell align="right">{formaterNOK(beregning.belop)}</Table.DataCell>
              <Table.DataCell align="right">
                <TilsagnTag status={tilsagn.status} />
              </Table.DataCell>
              <Table.DataCell>
                <Link to={`/gjennomforinger/${gjennomforingId}/tilsagn/${id}`}>
                  {[
                    TilsagnStatus.ANNULLERT,
                    TilsagnStatus.GODKJENT,
                    TilsagnStatus.OPPGJORT,
                  ].includes(tilsagn.status)
                    ? "Detaljer"
                    : "Behandle"}
                </Link>
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}

function getAntallPlasser(tilsagn: TilsagnDto) {
  return isBeregningPrisPerManedsverk(tilsagn.beregning) ? tilsagn.beregning.antallPlasser : null;
}
