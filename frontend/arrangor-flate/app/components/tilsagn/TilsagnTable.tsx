import { ArrangorflateTilsagn, TilsagnType } from "api-client";
import { Alert, Table } from "@navikt/ds-react";
import { formaterPeriode, useOrgnrFromUrl } from "~/utils";
import { internalNavigation } from "../../internal-navigation";
import { LinkWithTabState } from "../LinkWithTabState";
import { TilsagnStatusTag } from "./TilsagnStatusTag";

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
              <Table.HeaderCell>Tilsagnstype</Table.HeaderCell>
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
                  <Table.DataCell>{formaterTilsagnType(tilsagn.type)}</Table.DataCell>
                  <Table.DataCell>{formaterPeriode(tilsagn.periode)}</Table.DataCell>
                  <Table.DataCell>
                    <TilsagnStatusTag data={tilsagn.status} />
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

function formaterTilsagnType(type: TilsagnType): string {
  switch (type) {
    case TilsagnType.TILSAGN:
      return "Tilsagn";
    case TilsagnType.EKSTRATILSAGN:
      return "Ekstratilsagn";
    case TilsagnType.INVESTERING:
      return "Tilsagn for investeringer";
  }
}
