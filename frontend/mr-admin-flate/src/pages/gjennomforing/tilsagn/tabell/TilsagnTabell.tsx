import { TilsagnTag } from "@/pages/gjennomforing/tilsagn/TilsagnTag";
import { isTilsagnForhandsgodkjent } from "@/pages/gjennomforing/tilsagn/tilsagnUtils";
import { formaterDato } from "@/utils/Utils";
import { TilsagnDto } from "@mr/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { SortState, Table } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useState } from "react";
import { Link, useParams } from "react-router";

interface TabellData extends TilsagnDto {
  antallPlasser: number | null;
  navnForKostnadssted: string;
  belop: number;
}

interface Props {
  tilsagn: TilsagnDto[];
}

interface ScopedSortState extends SortState {
  orderBy: keyof TilsagnDto;
}

export function TilsagnTabell({ tilsagn }: Props) {
  const { tiltaksgjennomforingId } = useParams();

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

  function comparator<T>(a: T, b: T, orderBy: keyof T): number {
    const aValue = a[orderBy];
    const bValue = b[orderBy];
    if (bValue == null) {
      return -1;
    }

    if (typeof aValue === "number" && typeof bValue === "number") {
      return bValue - aValue;
    }

    if (bValue < aValue) {
      return -1;
    }
    if (bValue > aValue) {
      return 1;
    }
    return 0;
  }
  const sortedData: TabellData[] = [...tilsagn]
    .map((tilsagn) => ({
      ...tilsagn,
      antallPlasser: getAntallPlasser(tilsagn),
      navnForKostnadssted: tilsagn.kostnadssted.navn,
      belop: tilsagn.beregning.output.belop,
    }))
    .sort((a, b) => {
      if (sort) {
        return sort.direction === "ascending"
          ? comparator(b, a, sort.orderBy)
          : comparator(a, b, sort.orderBy);
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
          <TableColumnHeader sortKey="periodeStart" sortable>
            Periodestart
          </TableColumnHeader>
          <TableColumnHeader sortKey="periodeSlutt" sortable>
            Periodeslutt
          </TableColumnHeader>
          <TableColumnHeader sortKey="navnForKostnadssted" sortable>
            Kostnadssted
          </TableColumnHeader>
          <TableColumnHeader sortKey="antallPlasser" sortable align="right">
            Antall plasser
          </TableColumnHeader>
          <TableColumnHeader sortKey="belop" sortable align="right">
            Beløp
          </TableColumnHeader>
          <TableColumnHeader sortKey={"status"} sortable align="right">
            Status
          </TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((tilsagn) => {
          const { periodeStart, periodeSlutt, kostnadssted, beregning, id } = tilsagn;
          return (
            <Table.Row key={id}>
              <Table.DataCell>{formaterDato(periodeStart)}</Table.DataCell>
              <Table.DataCell>{formaterDato(periodeSlutt)}</Table.DataCell>
              <Table.DataCell>{kostnadssted.navn}</Table.DataCell>
              <Table.DataCell align="right">{getAntallPlasser(tilsagn)}</Table.DataCell>
              <Table.DataCell align="right">{formaterNOK(beregning.output.belop)}</Table.DataCell>
              <Table.DataCell align="right">
                <TilsagnTag status={tilsagn.status} />
              </Table.DataCell>

              <Table.DataCell>
                <Link to={`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn/${id}`}>
                  Detaljer
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
  return isTilsagnForhandsgodkjent(tilsagn) ? tilsagn.beregning.input.antallPlasser : null;
}
