import { formaterDato } from "@/utils/Utils";
import { TilsagnDto } from "@mr/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { SortState, Table } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { isAftBeregning } from "./tilsagnUtils";
import { TilsagnTag } from "./TilsagnTag";

interface Props {
  tilsagn: TilsagnDto[];
}

interface ScopedSortState extends SortState {
  orderBy: keyof TilsagnDto;
}

export function Tilsagnstabell({ tilsagn }: Props) {
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
    if (b[orderBy] == null || b[orderBy] < a[orderBy]) {
      return -1;
    }
    if (b[orderBy] > a[orderBy]) {
      return 1;
    }
    return 0;
  }

  const sortedData = [...tilsagn].sort((a, b) => {
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
          <TableColumnHeader sortKey="kostnadssted.navn" sortable>
            Kostnadssted
          </TableColumnHeader>
          <TableColumnHeader sortKey="tiltaksgjennomforing.antallPlasser" sortable>
            Antall plasser
          </TableColumnHeader>
          <TableColumnHeader sortKey="beregning.belop" sortable>
            Beløp
          </TableColumnHeader>
          <TableColumnHeader sortKey={"status"} sortable>
            Status
          </TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map((tilsagn) => {
          const { periodeStart, periodeSlutt, kostnadssted, beregning, id } = tilsagn;

          const antallPlasser = getAntallPlasser(beregning);
          return (
            <Table.Row key={id}>
              <Table.DataCell>{formaterDato(periodeStart)}</Table.DataCell>
              <Table.DataCell>{formaterDato(periodeSlutt)}</Table.DataCell>
              <Table.DataCell>{kostnadssted.navn}</Table.DataCell>
              <Table.DataCell>{antallPlasser}</Table.DataCell>
              <Table.DataCell>{formaterNOK(beregning.belop)}</Table.DataCell>
              <Table.DataCell>
                <TilsagnTag tilsagn={tilsagn} />
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

function getAntallPlasser(beregning: TilsagnDto["beregning"]) {
  if (isAftBeregning(beregning)) {
    return beregning.antallPlasser;
  }
  return 0;
}
