import { RefusjonKravAft, RefusjonskravStatus } from "@mr/api-client";
import { Alert, Table, Tag } from "@navikt/ds-react";
import { Link } from "@remix-run/react";
import React, { ReactNode } from "react";
import { formaterDato } from "~/utils";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

interface Props {
  krav: RefusjonKravAft[];
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
            <Table.HeaderCell scope="col">Periode</Table.HeaderCell>
            <Table.HeaderCell scope="col">Månedsverk</Table.HeaderCell>
            <Table.HeaderCell scope="col">Beløp</Table.HeaderCell>
            <Table.HeaderCell scope="col">Frist for godkjenning</Table.HeaderCell>
            <Table.HeaderCell scope="col">Status</Table.HeaderCell>
            <Table.HeaderCell scope="col"></Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <div className="not-sr-only mb-5 opacity-0 aria-hidden"></div>
        <Table.Body>
          {krav.map(
            ({ id, status, fristForGodkjenning, beregning, gjennomforing, tiltakstype }, index) => {
              return (
                <React.Fragment key={id}>
                  <Table.Row
                    id={`row${index + 1}-header`}
                    className="border-border-divider border-t-2 border-x-2 border-b-0"
                  >
                    <Table.HeaderCell
                      className="w-full bg-bg-subtle border-b-0"
                      colSpan={6}
                      scope="rowgroup"
                      aria-label={`${tiltakstype.navn} - ${gjennomforing.navn}`}
                    >
                      {tiltakstype.navn} - {gjennomforing.navn}
                    </Table.HeaderCell>
                  </Table.Row>
                  <Table.Row
                    aria-labelledby={`row${index + 1}-header`}
                    className={
                      getRowStyle(status) +
                      " pb-10 border-border-divider border-b-2 border-x-2 border-t-0"
                    }
                  >
                    <Table.DataCell>
                      {`${formaterDato(beregning.periodeStart)} - ${formaterDato(beregning.periodeSlutt)}`}
                    </Table.DataCell>
                    <Table.DataCell>{beregning.antallManedsverk}</Table.DataCell>
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
                  <div className="not-sr-only mb-5 opacity-0 aria-hidden"></div>
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
