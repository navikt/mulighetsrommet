import { ClockIcon } from "@navikt/aksel-icons";
import { Alert, Button, HelpText, HStack, Table } from "@navikt/ds-react";
import { NavAnsatt, TilsagnBesluttelse, TilsagnDto } from "mulighetsrommet-api-client";
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
    return tilsagn.reduce((acc, tilsagn) => acc + tilsagn.belop, 0);
  }

  function tilsagnTilStatus(tilsagn: TilsagnDto, ansatt?: NavAnsatt) {
    const { besluttelse, opprettetAv, id } = tilsagn;

    const tilsagnOpprettetAvBruker = ansatt?.navIdent === opprettetAv;
    const tilsagnUtenBesluttelse = !besluttelse;

    const statuser = () => {
      if (besluttelse) {
        return (
          <Alert
            inline
            size="small"
            variant={besluttelse.utfall === "GODKJENT" ? "success" : "error"}
          >
            <HStack justify={"space-between"} gap="2" align={"center"}>
              {besluttelseTilTekst(besluttelse.utfall)}{" "}
              <HelpText>
                {besluttelseTilTekst(besluttelse.utfall)} den {formaterDato(besluttelse.tidspunkt)}{" "}
                av {besluttelse.navIdent}
              </HelpText>
            </HStack>
          </Alert>
        );
      }
    };

    const besluttKnapp = () => {
      return (
        <>
          {statuser()}
          <Button type="button" variant="primary" size="small" onClick={() => besluttTilsagn(id)}>
            Beslutt
          </Button>
        </>
      );
    };

    const tilBeslutning = () => {
      return (
        <span>
          <HStack align={"center"} gap="1">
            <ClockIcon /> Til beslutning
          </HStack>
        </span>
      );
    };

    if (tilsagnOpprettetAvBruker) {
      if (tilsagnUtenBesluttelse) {
        return tilBeslutning();
      }
      return statuser();
    } else {
      if (tilsagnUtenBesluttelse) {
        return besluttKnapp();
      } else {
        return statuser();
      }
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
          const { periodeStart, periodeSlutt, kostnadssted, belop, id, besluttelse } = tilsagn;
          return (
            <Table.Row key={id}>
              <Table.DataCell>{formaterDato(periodeStart)}</Table.DataCell>
              <Table.DataCell>{formaterDato(periodeSlutt)}</Table.DataCell>
              <Table.DataCell>
                {kostnadssted.navn} {kostnadssted.enhetsnummer}
              </Table.DataCell>
              <Table.DataCell>{formaterTall(belop)} kr</Table.DataCell>
              <Table.DataCell>{tilsagnTilStatus(tilsagn, ansatt)}</Table.DataCell>
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
