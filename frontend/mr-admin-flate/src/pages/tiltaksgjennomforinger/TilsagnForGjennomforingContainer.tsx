import { Alert, Table } from "@navikt/ds-react";
import { TilsagnDto } from "mulighetsrommet-api-client";
import { Link } from "react-router-dom";
import { useHentTilsagnForTiltaksgjennomforing } from "../../api/tilsagn/useHentTilsagnForTiltaksgjennomforing";
import { Laster } from "../../components/laster/Laster";
import { InfoContainer } from "../../components/skjema/InfoContainer";
import { useGetTiltaksgjennomforingIdFromUrl } from "../../hooks/useGetTiltaksgjennomforingIdFromUrl";
import { formaterDato, formaterTall } from "../../utils/Utils";

export function TilsagnForGjennomforingContainer() {
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFromUrl();
  const { data: tilsagn, isLoading } =
    useHentTilsagnForTiltaksgjennomforing(tiltaksgjennomforingId);

  if (!tilsagn && isLoading) {
    return <Laster tekst="Laster tilsagn" />;
  }

  if (!tilsagn) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltaksgjennomføring
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  return (
    <InfoContainer>
      {tilsagn.length > 0 ? (
        <Tilsagnstabell tilsagn={tilsagn} />
      ) : (
        <Alert variant="info">Det finnes ingen tilsagn for dette tiltaket</Alert>
      )}
    </InfoContainer>
  );
}

function Tilsagnstabell({ tilsagn }: { tilsagn: TilsagnDto[] }) {
  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Periodestart</Table.HeaderCell>
          <Table.HeaderCell scope="col">Periodeslutt</Table.HeaderCell>
          <Table.HeaderCell scope="col">Beløp</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {tilsagn.map(({ periodeStart, periodeSlutt, belop, id }) => {
          return (
            <Table.Row key={id}>
              <Table.DataCell scope="row">{formaterDato(periodeStart)}</Table.DataCell>
              <Table.DataCell>{formaterDato(periodeSlutt)}</Table.DataCell>
              <Table.DataCell>{formaterTall(belop)} kr</Table.DataCell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table>
  );
}
