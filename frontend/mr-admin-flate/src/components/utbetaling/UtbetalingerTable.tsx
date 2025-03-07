import { formaterDato } from "@/utils/Utils";
import { AdminUtbetalingStatus, UtbetalingKompakt } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { SortState, Table } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useState } from "react";
import { Link, useParams } from "react-router";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";

interface Props {
  utbetalinger: UtbetalingKompakt[];
}

interface ScopedSortState extends SortState {
  orderBy: keyof UtbetalingKompakt;
}

interface TabellData extends UtbetalingKompakt {
  periode: Date;
  belop: number;
  status: AdminUtbetalingStatus;
}

export function UtbetalingerTable({ utbetalinger }: Props) {
  const { gjennomforingId } = useParams();

  const [sort, setSort] = useState<ScopedSortState | undefined>();

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

    if (aValue instanceof Date && bValue instanceof Date) {
      return bValue.getTime() - aValue.getTime();
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

  const sortedData: TabellData[] = [...utbetalinger]
    .map((utbetaling) => ({
      ...utbetaling,
      periode: new Date(utbetaling.beregning.periodeStart),
      belop: utbetaling.beregning.belop,
      status: utbetaling.status,
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
          <TableColumnHeader sortKey="periode" sortable>
            Periode
          </TableColumnHeader>
          <TableColumnHeader sortKey="belop" sortable align="right">
            Beløp
          </TableColumnHeader>
          <TableColumnHeader sortKey="status" sortable align="right">
            Status
          </TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map(({ beregning, id, status }) => {
          return (
            <Table.Row key={id}>
              <Table.DataCell>{`${formaterDato(beregning.periodeStart)}-${formaterDato(beregning.periodeSlutt)}`}</Table.DataCell>
              <Table.DataCell align="right">{formaterNOK(beregning.belop)}</Table.DataCell>
              <Table.DataCell align="right">
                <UtbetalingStatusTag status={status} />
              </Table.DataCell>
              <Table.DataCell>
                {status !== AdminUtbetalingStatus.VENTER_PA_ARRANGOR && (
                  <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger/${id}`}>
                    Detaljer
                  </Link>
                )}
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
