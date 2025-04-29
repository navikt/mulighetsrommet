import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, Table } from "@navikt/ds-react";
import React from "react";
import { formaterDato, formaterPeriode, useOrgnrFromUrl } from "~/utils";
import { internalNavigation } from "~/internal-navigation";
import { LinkWithTabState } from "../LinkWithTabState";
import { ArrFlateUtbetalingKompakt, ArrFlateUtbetalingStatus } from "api-client";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";

interface Props {
  utbetalinger: ArrFlateUtbetalingKompakt[];
}

export function UtbetalingTable({ utbetalinger }: Props) {
  const orgnr = useOrgnrFromUrl();

  if (utbetalinger.length === 0) {
    return (
      <Alert className="my-10 mt-10" variant="info">
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
          <Table.HeaderCell align="right" scope="col">
            Periode
          </Table.HeaderCell>
          <Table.HeaderCell align="right" scope="col" className="min-w-32">
            Beløp
          </Table.HeaderCell>
          <Table.HeaderCell align="right" scope="col">
            Frist for godkjenning
          </Table.HeaderCell>
          <Table.HeaderCell scope="col" className="min-w-44">
            Status
          </Table.HeaderCell>
          <Table.HeaderCell scope="col" className="w-10"></Table.HeaderCell>
          <Table.HeaderCell scope="col"></Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {utbetalinger.map(
          ({ id, status, fristForGodkjenning, belop, periode, gjennomforing, tiltakstype }) => {
            return (
              <React.Fragment key={id}>
                <Table.Row>
                  <Table.DataCell>{tiltakstype.navn}</Table.DataCell>
                  <Table.DataCell>{gjennomforing.navn}</Table.DataCell>
                  <Table.DataCell align="right">{formaterPeriode(periode)}</Table.DataCell>
                  <Table.DataCell align="right">{formaterNOK(belop)}</Table.DataCell>
                  <Table.DataCell align="right">{formaterDato(fristForGodkjenning)}</Table.DataCell>
                  <Table.DataCell>
                    <UtbetalingStatusTag status={status} />
                  </Table.DataCell>
                  <Table.DataCell />
                  <Table.DataCell>
                    <LinkWithTabState
                      aria-label={`Detaljer for krav om utbetaling for ${gjennomforing.navn}`}
                      className="hover:underline font-bold no-underline"
                      to={
                        [
                          ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING,
                          ArrFlateUtbetalingStatus.VENTER_PA_ENDRING,
                        ].includes(status)
                          ? internalNavigation(orgnr).beregning(id)
                          : internalNavigation(orgnr).detaljer(id)
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
