import { formaterDato } from "@/utils/Utils";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Table } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { Link, useParams } from "react-router";
import { UtbetalingKompakt, UtbetalingStatus } from "@mr/api-client-v2";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";

interface Props {
  utbetalinger: UtbetalingKompakt[];
}

export function UtbetalingerTable({ utbetalinger }: Props) {
  const { gjennomforingId } = useParams();

  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <TableColumnHeader sortKey="periodeStart" sortable>
            Periode
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
        {utbetalinger.map(({ beregning, id, status }) => {
          return (
            <Table.Row key={id}>
              <Table.DataCell>{`${formaterDato(beregning.periodeStart)}-${formaterDato(beregning.periodeSlutt)}`}</Table.DataCell>
              <Table.DataCell align="right">{formaterNOK(beregning.belop)}</Table.DataCell>
              <Table.DataCell align="right">
                <UtbetalingStatusTag status={status} />
              </Table.DataCell>
              <Table.DataCell>
                { status !== UtbetalingStatus.KLAR_FOR_GODKJENNING &&
                  <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger/${id}`}>Detaljer</Link>
                }
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
