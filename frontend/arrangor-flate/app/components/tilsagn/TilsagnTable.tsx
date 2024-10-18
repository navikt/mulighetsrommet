import { ArrangorflateTilsagn } from "@mr/api-client";
import { Table } from "@navikt/ds-react";
import { Link } from "@remix-run/react";
import { formaterDato } from "~/utils";

interface Props {
  tilsagn: ArrangorflateTilsagn[];
}

export function TilsagnTable({ tilsagn }: Props) {
  return (
    <>
      <div className="border-spacing-y-6 border-collapsed mt-4">
        <Table zebraStripes>
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
                    <Link
                      className="hover:underline font-bold no-underline"
                      to={`tilsagn/${tilsagn.id}`}
                    >
                      Detaljer
                    </Link>
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
