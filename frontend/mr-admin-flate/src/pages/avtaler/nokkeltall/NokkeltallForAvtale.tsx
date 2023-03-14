import { Alert } from "@navikt/ds-react";
import { useNokkeltallForAvtale } from "../../../api/avtaler/useNokkeltallForAvtale";
import { Laster } from "../../../components/Laster";
import { Nokkeltall } from "../../../components/nokkeltall/Nokkeltall";
import { NokkeltallContainer } from "../../../components/nokkeltall/NokkeltallContainer";
import { formaterTall } from "../../../utils/Utils";

export function NokkeltallForAvtale() {
  const { data, isLoading } = useNokkeltallForAvtale();

  if (isLoading && !data) {
    return <Laster tekst={"Henter nøkkeltall..."} />;
  }

  if (!data) {
    return <Alert variant={"error"}>Fant ingen nøkkeltall</Alert>;
  }

  return (
    <NokkeltallContainer>
      <Nokkeltall
        title="Tiltaksgjennomføringer"
        subtitle="totalt"
        value={formaterTall(data.antallTiltaksgjennomforinger)}
        helptext="Totalt antall tiltaksgjennomføringer"
        helptextTitle="Hvor kommer tallene fra?"
      />
    </NokkeltallContainer>
  );
}
