import {
  compareByKey,
  formaterNavEnheter,
  formaterPeriodeSlutt,
  formaterPeriodeStart,
} from "@/utils/Utils";
import { UtbetalingDto, AdminUtbetalingStatus } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { SortState, Table } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { useState } from "react";
import { Link, useParams } from "react-router";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";

interface Props {
  utbetalinger: UtbetalingDto[];
}

interface ScopedSortState extends SortState {
  orderBy: keyof UtbetalingDto;
}

interface UtbetalingTabellData extends UtbetalingDto {
  periodeStart: string;
  periodeSlutt: string;
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

  const sortedData: UtbetalingTabellData[] = [...utbetalinger]
    .map((utbetaling) => ({
      ...utbetaling,
      periodeStart: utbetaling.periode.start,
      periodeSlutt: utbetaling.periode.slutt,
      belop: utbetaling.beregning.belop,
      status: utbetaling.status,
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
      aria-label="Utbetalinger"
    >
      <Table.Header>
        <Table.Row>
          <TableColumnHeader sortKey="periodeStart" sortable>
            {utbetalingTekster.periode.start.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="periodeSlutt" sortable>
            {utbetalingTekster.periode.slutt.label}
          </TableColumnHeader>
          <Table.ColumnHeader>Kostnadssteder</Table.ColumnHeader>
          <TableColumnHeader sortKey="belop" sortable align="right">
            {utbetalingTekster.beregning.belop.label}
          </TableColumnHeader>
          <TableColumnHeader sortKey="status" sortable align="right">
            Status
          </TableColumnHeader>
          <TableColumnHeader></TableColumnHeader>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {sortedData.map(({ beregning, periode, id, status, kostnadssteder }) => {
          return (
            <Table.Row key={id}>
              <Table.DataCell>{formaterPeriodeStart(periode)}</Table.DataCell>
              <Table.DataCell>{formaterPeriodeSlutt(periode)}</Table.DataCell>
              <Table.DataCell
                aria-label={`Kostnadssteder: ${kostnadssteder
                  .map((kostnadssted) => kostnadssted.navn)
                  .join(", ")}`}
                title={`Kostnadssteder: ${kostnadssteder
                  .map((kostnadssted) => kostnadssted.navn)
                  .join(", ")}`}
              >
                {formaterNavEnheter(
                  kostnadssteder.map((kostnadssted) => ({
                    navn: kostnadssted.navn,
                    enhetsnummer: kostnadssted.enhetsnummer,
                  })),
                )}
              </Table.DataCell>
              <Table.DataCell align="right">{formaterNOK(beregning.belop)}</Table.DataCell>
              <Table.DataCell align="right">
                <UtbetalingStatusTag status={status} />
              </Table.DataCell>
              <Table.DataCell>
                {status !== AdminUtbetalingStatus.VENTER_PA_ARRANGOR && (
                  <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger/${id}`}>
                    {[
                      AdminUtbetalingStatus.UTBETALT,
                      AdminUtbetalingStatus.OVERFORT_TIL_UTBETALING,
                    ].includes(status)
                      ? "Detaljer"
                      : "Behandle"}
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
