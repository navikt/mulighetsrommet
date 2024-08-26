import { ClockIcon } from "@navikt/aksel-icons";
import { Alert, Button, HelpText, HStack, Table } from "@navikt/ds-react";
import { NavAnsatt, NavAnsattRolle, TilsagnBesluttelse, TilsagnDto } from "@mr/api-client";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { formaterDato, formaterTall } from "../../../utils/Utils";

interface Props {
  tilsagn: TilsagnDto[];
}

export function Tilsagnstabell({ tilsagn }: Props) {
  const { tiltaksgjennomforingId } = useParams();
  const { data: ansatt } = useHentAnsatt();
  const navigate = useNavigate();

  function besluttTilsagn(id: string) {
    navigate(id);
  }

  function redigerTilsagn(id: string) {
    navigate(`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn/${id}/rediger-tilsagn`);
  }

  function totalSum(tilsagn: TilsagnDto[]): number {
    return tilsagn.reduce((acc, tilsagn) => acc + tilsagn.beregning.belop, 0);
  }

  function status(tilsagn: TilsagnDto, ansatt?: NavAnsatt) {
    const { besluttelse, id, opprettetAv, annullertTidspunkt } = tilsagn;

    if (besluttelse) {
      return (
        <Alert
          inline
          size="small"
          variant={besluttelse.utfall === "GODKJENT" ? "success" : "warning"}
        >
          <HStack justify={"space-between"} gap="2" align={"center"}>
            {besluttelseTilTekst(besluttelse.utfall)}{" "}
            <HelpText>
              {besluttelseTilTekst(besluttelse.utfall)} den {formaterDato(besluttelse.tidspunkt)} av{" "}
              {besluttelse.navIdent}
            </HelpText>
          </HStack>
        </Alert>
      );
    } else if (annullertTidspunkt) {
      return (
        <HStack justify={"space-between"} gap="2" align={"center"}>
          Annullert
          <HelpText>{`Annullert den ${formaterDato(annullertTidspunkt)}`}</HelpText>
        </HStack>
      );
    } else if (
      ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER) &&
      opprettetAv !== ansatt?.navIdent
    ) {
      return (
        <Button type="button" variant="primary" size="small" onClick={() => besluttTilsagn(id)}>
          Beslutt
        </Button>
      );
    } else {
      return (
        <span>
          <HStack align={"center"} gap="1">
            <ClockIcon /> Til beslutning
          </HStack>
        </span>
      );
    }
  }

  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Periodestart</Table.HeaderCell>
          <Table.HeaderCell>Periodeslutt</Table.HeaderCell>
          <Table.HeaderCell>Kostnadssted</Table.HeaderCell>
          <Table.HeaderCell>
            Beløp <small>(totalt {formaterTall(totalSum(tilsagn))} kr)</small>
          </Table.HeaderCell>
          <Table.HeaderCell></Table.HeaderCell>
          <Table.HeaderCell></Table.HeaderCell>
          <Table.HeaderCell></Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tilsagn.map((tilsagn) => {
          const { periodeStart, periodeSlutt, kostnadssted, beregning, id, besluttelse } = tilsagn;
          return (
            <Table.Row key={id}>
              <Table.DataCell>{formaterDato(periodeStart)}</Table.DataCell>
              <Table.DataCell>{formaterDato(periodeSlutt)}</Table.DataCell>
              <Table.DataCell>
                {kostnadssted.navn} {kostnadssted.enhetsnummer}
              </Table.DataCell>
              <Table.DataCell>{formaterTall(beregning.belop)} kr</Table.DataCell>
              <Table.DataCell>{status(tilsagn, ansatt)}</Table.DataCell>
              <Table.DataCell>
                {tilsagn?.opprettetAv === ansatt?.navIdent &&
                besluttelse?.utfall === TilsagnBesluttelse.AVVIST ? (
                  <Button
                    type="button"
                    variant="primary"
                    size="small"
                    onClick={() => redigerTilsagn(id)}
                  >
                    Korriger
                  </Button>
                ) : null}
              </Table.DataCell>
              <Table.DataCell>
                <Link to={`/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn/${id}`}>
                  Se tilsagn
                </Link>
              </Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}

function besluttelseTilTekst(besluttelse: TilsagnBesluttelse): "Godkjent" | "Avvist" {
  return besluttelse === "GODKJENT" ? "Godkjent" : "Avvist";
}
