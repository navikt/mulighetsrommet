import { RefusjonKravKompakt, RefusjonskravStatus } from "@mr/api-client";
import { Alert, Table, Tag } from "@navikt/ds-react";
import { Link } from "@remix-run/react";
import React, { ReactNode } from "react";
import { formaterDato } from "~/utils";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

interface Props {
  krav: RefusjonKravKompakt[];
}

export function RefusjonskravTable({ krav }: Props) {
  if (krav.length === 0) {
    return (
      <Alert className="my-10" variant="info">
        Det finnes ingen refusjonskrav her
      </Alert>
    );
  }

  return (
    <>
      <Table aria-label="Refusjonskrav">
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
          {krav.map(
            ({ id, status, fristForGodkjenning, beregning, gjennomforing, tiltakstype }) => {
              return (
                <React.Fragment key={id}>
                  <Table.Row
                    className={
                      getRowStyle(status) +
                      " pb-10 border-border-divider border-b-2 border-x-2 border-t-0"
                    }
                  >
                    <Table.DataCell>{tiltakstype.navn}</Table.DataCell>
                    <Table.DataCell>{gjennomforing.navn}</Table.DataCell>
                    <Table.DataCell colSpan={3}>
                      {`${formaterDato(beregning.periodeStart)} - ${formaterDato(beregning.periodeSlutt)}`}
                    </Table.DataCell>
                    <Table.DataCell>{formaterNOK(beregning.belop)}</Table.DataCell>
                    <Table.DataCell>{formaterDato(fristForGodkjenning)}</Table.DataCell>
                    <Table.DataCell>{statusTilTag(status)}</Table.DataCell>
                    <Table.DataCell>
                      <Link
                        className="hover:underline font-bold no-underline"
                        to={`/refusjonskrav/${id}/for-du-begynner`}
                      >
                        Detaljer
                      </Link>
                    </Table.DataCell>
                  </Table.Row>
                </React.Fragment>
              );
            },
          )}
        </Table.Body>
      </Table>
    </>
  );
}

function getRowStyle(status: RefusjonskravStatus) {
  return status === RefusjonskravStatus.NARMER_SEG_FRIST ? "bg-surface-warning-moderate" : "";
}

function statusTilTag(status: RefusjonskravStatus): ReactNode {
  switch (status) {
    case RefusjonskravStatus.GODKJENT_AV_ARRANGOR:
      return <Tag variant="neutral">Godkjent</Tag>;
    case RefusjonskravStatus.KLAR_FOR_GODKJENNING:
      return <Tag variant="alt1">Klar for innsending</Tag>;
    case RefusjonskravStatus.NARMER_SEG_FRIST:
      return <Tag variant="warning">Nærmer seg frist</Tag>;
  }
}
