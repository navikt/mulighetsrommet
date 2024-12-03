import { Alert, Heading } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useHentTilsagnForTiltaksgjennomforing } from "../../../api/tilsagn/useHentTilsagnForTiltaksgjennomforing";
import { Laster } from "../../../components/laster/Laster";
import { InfoContainer } from "../../../components/skjema/InfoContainer";
import { useGetTiltaksgjennomforingIdFromUrl } from "../../../hooks/useGetTiltaksgjennomforingIdFromUrl";
import { Tilsagnstabell } from "./Tilsagnstabell";
import { OpprettTilsagn } from "@/pages/tiltaksgjennomforinger/tilsagn/OpprettTilsagn";

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
    <>
      <InfoContainer>
        {tilsagn.length > 0 ? (
          <Tilsagnstabell tilsagn={tilsagn} />
        ) : (
          <Alert variant="info">Det finnes ingen tilsagn for dette tiltaket</Alert>
        )}
      </InfoContainer>
    </>
  );
}
