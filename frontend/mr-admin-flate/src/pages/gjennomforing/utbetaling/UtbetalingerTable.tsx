import { formaterDato } from "@/utils/Utils";
import { Utbetaling } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Table } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { Link, useParams } from "react-router";
import { RefusjonskravStatusTag } from "./RefusjonskravStatusTag";

interface Props {
  utbetalinger: Utbetaling[];
}

export function UtbetalingerTable({ utbetalinger }: Props) {
  const { gjennomforingId } = useParams();

  function formaterKostnadsteder(
    kostnadsteder: {
      navn: string;
      enhetsnummer: string;
    }[],
  ) {
    const liste = [...kostnadsteder];
    if (!liste) return "";

    const forsteEnhet = liste.shift();
    if (!forsteEnhet) return "";

    return `${forsteEnhet?.navn} ${liste.length > 0 ? `+ ${liste.length}` : ""}`;
  }

  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <TableColumnHeader sortKey="periodeStart" sortable>
            Periode
          </TableColumnHeader>
          <TableColumnHeader sortKey="navnForKostnadssted" sortable>
            Kostnadssted
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
        {utbetalinger.map(({ krav, utbetalinger }) => {
          const { beregning, id, status } = krav;

          return (
            <Table.Row key={id}>
              <Table.DataCell>{`${formaterDato(beregning.periodeStart)}-${formaterDato(beregning.periodeSlutt)}`}</Table.DataCell>
              <Table.DataCell
                aria-label={`Kostnadsteder: ${krav.kostnadsteder
                  .map((enhet) => enhet?.navn)
                  .join(", ")}`}
                title={`Kostnadsteder: ${krav.kostnadsteder
                  .map((enhet) => enhet?.navn)
                  .join(", ")}`}
              >
                {formaterKostnadsteder(krav.kostnadsteder)}
              </Table.DataCell>
              <Table.DataCell align="right">{formaterNOK(beregning.belop)}</Table.DataCell>
              <Table.DataCell align="right">
                <RefusjonskravStatusTag status={status} />
              </Table.DataCell>
              <Table.DataCell>
                {utbetalinger.length > 0 ? (
                  <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger/${id}`}>
                    Detaljer
                  </Link>
                ) : (
                  <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger/${id}/skjema`}>
                    Behandle
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
