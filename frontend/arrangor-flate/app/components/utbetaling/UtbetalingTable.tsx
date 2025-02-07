import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, Table, Tag } from "@navikt/ds-react";
import React, { ReactNode } from "react";
import { formaterDato, useOrgnrFromUrl } from "~/utils";
import { internalNavigation } from "~/internal-navigation";
import { LinkWithTabState } from "../LinkWithTabState";
import { ArrFlateUtbetalingKompakt, UtbetalingStatus } from "@mr/api-client-v2";

interface Props {
  utbetalinger: ArrFlateUtbetalingKompakt[];
}

export function UtbetalingTable({ utbetalinger }: Props) {
  const orgnr = useOrgnrFromUrl();

  if (utbetalinger.length === 0) {
    return (
      <Alert className="my-10" variant="info">
        Det finnes ingen utbetalinger her
      </Alert>
    );
  }

  return (
    <Table aria-label="Utbetalinger">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Tiltakstype</Table.HeaderCell>
          <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
          <Table.HeaderCell scope="col" colSpan={3}>
            Periode
          </Table.HeaderCell>
          <Table.HeaderCell scope="col">Beløp</Table.HeaderCell>
          <Table.HeaderCell scope="col">Frist for godkjenning</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
          <Table.HeaderCell scope="col"></Table.HeaderCell>
          <Table.HeaderCell scope="col"></Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {utbetalinger.map(
          ({
            id,
            status,
            fristForGodkjenning,
            belop,
            periodeStart,
            periodeSlutt,
            gjennomforing,
            tiltakstype,
          }) => {
            return (
              <React.Fragment key={id}>
                <Table.Row className={getRowStyle(status)}>
                  <Table.DataCell>{tiltakstype.navn}</Table.DataCell>
                  <Table.DataCell>{gjennomforing.navn}</Table.DataCell>
                  <Table.DataCell colSpan={3} className="w-80">
                    {`${formaterDato(periodeStart)} - ${formaterDato(periodeSlutt)}`}
                  </Table.DataCell>
                  <Table.DataCell className="min-w-44">{formaterNOK(belop)}</Table.DataCell>
                  <Table.DataCell>{formaterDato(fristForGodkjenning)}</Table.DataCell>
                  <Table.DataCell>{statusTilTag(status)}</Table.DataCell>
                  <Table.DataCell>
                    <LinkWithTabState
                      className="hover:underline font-bold no-underline"
                      to={
                        status === UtbetalingStatus.GODKJENT_AV_ARRANGOR
                          ? internalNavigation(orgnr).kvittering(id)
                          : internalNavigation(orgnr).beregning(id)
                      }
                    >
                      Detaljer
                    </LinkWithTabState>
                  </Table.DataCell>
                </Table.Row>
              </React.Fragment>
            );
          },
        )}
      </Table.Body>
    </Table>
  );
}

function getRowStyle(status: UtbetalingStatus) {
  return status === UtbetalingStatus.NARMER_SEG_FRIST ? "bg-surface-warning-moderate" : "";
}

function statusTilTag(status: UtbetalingStatus): ReactNode {
  switch (status) {
    case UtbetalingStatus.GODKJENT_AV_ARRANGOR:
      return <Tag variant="neutral">Godkjent</Tag>;
    case UtbetalingStatus.KLAR_FOR_GODKJENNING:
      return <Tag variant="alt1">Klar for innsending</Tag>;
    case UtbetalingStatus.NARMER_SEG_FRIST:
      return <Tag variant="warning">Nærmer seg frist</Tag>;
  }
}
