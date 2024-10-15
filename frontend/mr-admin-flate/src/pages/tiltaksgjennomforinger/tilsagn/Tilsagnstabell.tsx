import { ClockIcon } from "@navikt/aksel-icons";
import { Alert, Button, HelpText, HStack, Table } from "@navikt/ds-react";
import { NavAnsatt, NavAnsattRolle, TilsagnBesluttelse, TilsagnDto } from "@mr/api-client";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import { formaterDato } from "../../../utils/Utils";
import { formaterTall } from "@mr/frontend-common/utils/utils";

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

  function TilsagnStatus(props: { tilsagn: TilsagnDto; ansatt?: NavAnsatt }) {
    const { tilsagn, ansatt } = props;

    if (tilsagn.besluttelse) {
      return (
        <Alert
          inline
          size="small"
          variant={tilsagn.besluttelse.utfall === "GODKJENT" ? "success" : "warning"}
        >
          <HStack justify={"space-between"} gap="2" align={"center"}>
            {besluttelseTilTekst(tilsagn.besluttelse.utfall)}{" "}
            <HelpText>
              {besluttelseTilTekst(tilsagn.besluttelse.utfall)} den{" "}
              {formaterDato(tilsagn.besluttelse.tidspunkt)} av {tilsagn.besluttelse.navIdent}
            </HelpText>
          </HStack>
        </Alert>
      );
    } else if (tilsagn.annullertTidspunkt) {
      return (
        <HStack justify={"space-between"} gap="2" align={"center"}>
          Annullert
          <HelpText>{`Annullert den ${formaterDato(tilsagn.annullertTidspunkt)}`}</HelpText>
        </HStack>
      );
    } else if (
      ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER) &&
      tilsagn.opprettetAv !== ansatt?.navIdent
    ) {
      return (
        <Button
          type="button"
          variant="primary"
          size="small"
          onClick={() => besluttTilsagn(tilsagn.id)}
        >
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
              <Table.DataCell>
                <TilsagnStatus tilsagn={tilsagn} ansatt={ansatt} />
              </Table.DataCell>
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
