import { Nokkeltall } from "../../../components/nokkeltall/Nokkeltall";
import { useNokkeltallForTiltakstype } from "../../../api/tiltakstyper/useNokkeltallForTiltakstype";
import { Laster } from "../../../components/Laster";
import { formaterTall } from "../../../utils/Utils";
import { Alert } from "@navikt/ds-react";
import { NokkeltallContainer } from "../../../components/nokkeltall/NokkeltallContainer";

export function NokkeltallForTiltakstype() {
  const { data, isLoading } = useNokkeltallForTiltakstype();

  if (isLoading && !data) {
    return <Laster tekst={"Henter nøkkeltall..."} />;
  }

  if (!data) {
    return <Alert variant={"error"}>Fant ingen nøkkeltall</Alert>;
  }

  return (
    <NokkeltallContainer>
      <Nokkeltall
        title="Avtaler"
        subtitle="totalt"
        value={formaterTall(data.antallAvtaler)}
      />
      <Nokkeltall
        title="Gjennomføringer"
        subtitle="totalt"
        value={formaterTall(data.antallTiltaksgjennomforinger)}
      />
    </NokkeltallContainer>
  );
}
