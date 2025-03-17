import { TilsagnTag } from "@/pages/gjennomforing/tilsagn/TilsagnTag";
import { isTilsagnForhandsgodkjent } from "@/pages/gjennomforing/tilsagn/tilsagnUtils";
import { compareByKey, formaterPeriodeSlutt, formaterPeriodeStart } from "@/utils/Utils";
import { TilsagnDto } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { SortState, Table } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useState } from "react";
import { Link, useParams } from "react-router";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";

interface TilsagnTabellData extends TilsagnDto {
  periodeStart: string;
  periodeSlutt: string;
  navnForKostnadssted: string;
  antallPlasser: number | null;
  belop: number;
}

interface Props {
  tilsagn: TilsagnDto[];
}

interface ScopedSortState extends SortState {
  orderBy: keyof TilsagnTabellData;
}

export function TilsagnTabell({ tilsagn }: Props) {
  const { gjennomforingId } = useParams();

  const [sort, setSort] = useState<ScopedSortState>();

  const handleSort = (sortKey: ScopedSortState["orderBy"]) => {
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
  };

  const sortedData: TilsagnTabellData[] = [...tilsagn]
    .map((tilsagn) => ({
      ...tilsagn,
      periodeStart: tilsagn.periode.start,
      periodeSlutt: tilsagn.periode.slutt,
      type: tilsagn.type,
      navnForKostnadssted: tilsagn.kostnadssted.navn,
      antallPlasser: getAntallPlasser(tilsagn),
      belop: tilsagn.beregning.output.belop,
      status: tilsagn.status,
    }))
    .toSorted((a, b) => {
      if (sort) {
        return sort.direction === "ascending"
          ? compareByKey(b, a, sort.orderBy)
          : compareByKey(a, b, sort.orderBy);
      } else {
        return 0;
      }
    });

  return (
    <Table
      sort={sort}
      onSortChange={(sortKey) => handleSort(sortKey as ScopedSortState["orderBy"])}
    >
      <Table.Header>
        <Table.Row>
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
              <Table.DataCell>{formaterPeriodeStart(periode)}</Table.DataCell>
              <Table.DataCell>{formaterPeriodeSlutt(periode)}</Table.DataCell>
              <Table.DataCell>{avtaletekster.tilsagn.type(tilsagn.type)}</Table.DataCell>
              <Table.DataCell>{kostnadssted.navn}</Table.DataCell>
              <Table.DataCell align="right">{getAntallPlasser(tilsagn)}</Table.DataCell>
              <Table.DataCell align="right">{formaterNOK(beregning.output.belop)}</Table.DataCell>
              <Table.DataCell align="right">
                <TilsagnTag status={tilsagn.status} />
              </Table.DataCell>
              <Table.DataCell>
                <Link to={`/gjennomforinger/${gjennomforingId}/tilsagn/${id}`}>Detaljer</Link>
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}

function getAntallPlasser(tilsagn: TilsagnDto) {
  return isTilsagnForhandsgodkjent(tilsagn) ? tilsagn.beregning.input.antallPlasser : null;
}
