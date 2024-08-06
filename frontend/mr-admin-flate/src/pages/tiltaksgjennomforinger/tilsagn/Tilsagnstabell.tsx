import { Button, Table } from "@navikt/ds-react";
import { TilsagnDto } from "mulighetsrommet-api-client";
import { formaterDato, formaterTall } from "../../../utils/Utils";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { useNavigate } from "react-router-dom";

interface Props {
  tilsagn: TilsagnDto[];
}

export function Tilsagnstabell({ tilsagn }: Props) {
  const { data: ansatt } = useHentAnsatt();
  const navigate = useNavigate();

  function besluttTilsagn(id: string) {
    navigate(id);
  }

  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Periodestart</Table.HeaderCell>
          <Table.HeaderCell>Periodeslutt</Table.HeaderCell>
          <Table.HeaderCell>Kostnadssted</Table.HeaderCell>
          <Table.HeaderCell>Beløp</Table.HeaderCell>
          <Table.HeaderCell></Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tilsagn.map(({ periodeStart, periodeSlutt, kostnadssted, belop, id, opprettetAv }) => {
          return (
            <Table.Row key={id}>
              <Table.DataCell>{formaterDato(periodeStart)}</Table.DataCell>
              <Table.DataCell>{formaterDato(periodeSlutt)}</Table.DataCell>
              <Table.DataCell>
                {kostnadssted.navn} {kostnadssted.enhetsnummer}
              </Table.DataCell>
              <Table.DataCell>{formaterTall(belop)} kr</Table.DataCell>
              <Table.DataCell>
                {ansatt?.navIdent !== opprettetAv ? (
                  <Button
                    type="button"
                    variant="primary"
                    size="small"
                    onClick={() => besluttTilsagn(id)}
                  >
                    Beslutt
                  </Button>
                ) : (
                  ""
                )}
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
