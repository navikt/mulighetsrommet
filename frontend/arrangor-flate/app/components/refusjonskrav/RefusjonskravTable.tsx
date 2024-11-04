import { RefusjonKravKompakt, RefusjonskravStatus } from "@mr/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, Table, Tag } from "@navikt/ds-react";
import { Link } from "@remix-run/react";
import React, { ReactNode } from "react";
import { formaterDato, useOrgnrFromUrl } from "~/utils";
import { internalNavigation } from "../../internal-navigation";

interface Props {
  krav: RefusjonKravKompakt[];
}

export function RefusjonskravTable({ krav }: Props) {
  const orgnr = useOrgnrFromUrl();
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
                  <Table.Row className={getRowStyle(status)}>
                    <Table.DataCell>{tiltakstype.navn}</Table.DataCell>
                    <Table.DataCell>{gjennomforing.navn}</Table.DataCell>
                    <Table.DataCell colSpan={3} className="w-80">
                      {`${formaterDato(beregning.periodeStart)} - ${formaterDato(beregning.periodeSlutt)}`}
                    </Table.DataCell>
                    <Table.DataCell className="min-w-44">
                      {formaterNOK(beregning.belop)}
                    </Table.DataCell>
                    <Table.DataCell>{formaterDato(fristForGodkjenning)}</Table.DataCell>
                    <Table.DataCell>{statusTilTag(status)}</Table.DataCell>
                    <Table.DataCell>
                      <Link
                        className="hover:underline font-bold no-underline"
                        to={internalNavigation(orgnr).beregning(id)}
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
