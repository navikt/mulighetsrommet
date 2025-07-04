import { ArrangorflateTilsagn, TilsagnType } from "api-client";
import { Alert, Table } from "@navikt/ds-react";
import { formaterPeriode, useOrgnrFromUrl } from "~/utils";
import { pathByOrgnr } from "../../pathByOrgnr";
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
              <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Periode</Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Tiltakstype</Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Tilsagnsnummer</Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Tilsagnstype</Table.ColumnHeader>
              <Table.ColumnHeader scope="col" className="min-w-44">
                Status
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col" aria-label="handlinger"></Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {tilsagn.map((tilsagn, i) => {
              return (
                <Table.Row key={i}>
                  <Table.HeaderCell>{tilsagn.gjennomforing.navn}</Table.HeaderCell>
                  <Table.DataCell>{formaterPeriode(tilsagn.periode)}</Table.DataCell>
                  <Table.DataCell>{tilsagn.tiltakstype.navn}</Table.DataCell>
                  <Table.DataCell>{tilsagn.bestillingsnummer}</Table.DataCell>
                  <Table.DataCell>{formaterTilsagnType(tilsagn.type)}</Table.DataCell>
                  <Table.DataCell>
                    <TilsagnStatusTag data={tilsagn.status} />
                  </Table.DataCell>
                  <Table.DataCell>
                    <LinkWithTabState
                      aria-label={`Detaljer for tilsagn for ${tilsagn.gjennomforing.navn}`}
                      className="hover:underline font-bold no-underline"
                      to={pathByOrgnr(orgnr).tilsagn(tilsagn.id)}
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

export function formaterTilsagnType(type: TilsagnType): string {
  switch (type) {
    case TilsagnType.TILSAGN:
      return "Tilsagn";
    case TilsagnType.EKSTRATILSAGN:
      return "Ekstratilsagn";
    case TilsagnType.INVESTERING:
      return "Tilsagn for investeringer";
  }
}
