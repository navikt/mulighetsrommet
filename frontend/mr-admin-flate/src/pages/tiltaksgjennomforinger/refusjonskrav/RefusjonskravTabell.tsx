import { formaterDato } from "@/utils/Utils";
import { RefusjonKravKompakt, RefusjonskravStatus } from "@mr/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Table, Tag } from "@navikt/ds-react";
import { TableColumnHeader } from "@navikt/ds-react/Table";
import { Link, useParams } from "react-router";
import { ReactNode } from "react";

interface Props {
  refusjonskrav: RefusjonKravKompakt[];
}

export function RefusjonskravTabell({ refusjonskrav }: Props) {
  const { tiltaksgjennomforingId } = useParams();

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
        {refusjonskrav.map((krav) => {
          const { beregning, id, status } = krav;

          return (
            <Table.Row key={id}>
              <Table.DataCell>{`${formaterDato(beregning.periodeStart)}-${formaterDato(beregning.periodeSlutt)}`}</Table.DataCell>
              <Table.DataCell>kostnadsted</Table.DataCell>
              <Table.DataCell align="right">{formaterNOK(beregning.belop)}</Table.DataCell>
              <Table.DataCell align="right">
                <RefusjonskravStatusTag status={status} />
              </Table.DataCell>
              <Table.DataCell>
                <Link to={`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/refusjonskrav/${id}`}>
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

function RefusjonskravStatusTag({ status }: { status: RefusjonskravStatus }): ReactNode {
  switch (status) {
    case RefusjonskravStatus.GODKJENT_AV_ARRANGOR:
      return <Tag variant="neutral">Godkjent</Tag>;
    case RefusjonskravStatus.KLAR_FOR_GODKJENNING:
      return <Tag variant="alt1">Klar for innsending</Tag>;
    case RefusjonskravStatus.NARMER_SEG_FRIST:
      return <Tag variant="warning">Nærmer seg frist</Tag>;
  }
}
