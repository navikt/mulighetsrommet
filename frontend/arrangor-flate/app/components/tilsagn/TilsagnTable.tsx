import { ArrangorflateTilsagn, TilsagnStatus } from "@mr/api-client";
import { Alert, Table, Tag } from "@navikt/ds-react";
import { formaterDato, useOrgnrFromUrl } from "~/utils";
import { internalNavigation } from "../../internal-navigation";
import { LinkWithTabState } from "../LinkWithTabState";

interface Props {
  tilsagn: ArrangorflateTilsagn[];
}

export function TilsagnTable({ tilsagn }: Props) {
  const orgnr = useOrgnrFromUrl();

  if (tilsagn.length === 0) {
    return (
      <Alert className="my-10" variant="info">
        Det finnes ingen tilsagn her
      </Alert>
    );
  }

  return (
    <>
      <div className="border-spacing-y-6 border-collapsed mt-4">
        <Table zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Tiltakstype</Table.HeaderCell>
              <Table.HeaderCell>Navn</Table.HeaderCell>
              <Table.HeaderCell>Periode</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {tilsagn.map((tilsagn, i) => {
              return (
                <Table.Row key={i}>
                  <Table.DataCell>{tilsagn.tiltakstype.navn}</Table.DataCell>
                  <Table.DataCell>{tilsagn.gjennomforing.navn}</Table.DataCell>
                  <Table.DataCell>
                    {`${formaterDato(tilsagn.periodeStart)} - ${formaterDato(tilsagn.periodeSlutt)}`}
                  </Table.DataCell>
                  <Table.DataCell>
                    <StatusTag status={tilsagn.status} />
                  </Table.DataCell>
                  <Table.DataCell>
                    <LinkWithTabState
                      className="hover:underline font-bold no-underline"
                      to={internalNavigation(orgnr).tilsagn(tilsagn.id)}
                    >
                      Detaljer
                    </LinkWithTabState>
                  </Table.DataCell>
                </Table.Row>
              );
            })}
          </Table.Body>
        </Table>
      </div>
    </>
  );
}

function StatusTag({ status }: { status: TilsagnStatus }) {
  switch (status) {
    case TilsagnStatus.GODKJENT:
      return <Tag variant="success">Godkjent</Tag>;
    case TilsagnStatus.ANNULLERT:
      return <Tag variant="error">Annullert</Tag>;
    case TilsagnStatus.TIL_ANNULLERING:
      return <Tag variant="neutral">Til annullering</Tag>;
    default:
      return null;
  }
}
