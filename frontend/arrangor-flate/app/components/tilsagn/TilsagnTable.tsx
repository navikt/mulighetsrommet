import { ArrangorflateTilsagn } from "@mr/api-client";
import { Alert, Table } from "@navikt/ds-react";
import { Link } from "@remix-run/react";
import { formaterDato, useOrgnrFromUrl } from "~/utils";
import { internalNavigation } from "../../internal-navigation";

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
                      to={internalNavigation(orgnr).tilsagn(tilsagn.id)}
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
