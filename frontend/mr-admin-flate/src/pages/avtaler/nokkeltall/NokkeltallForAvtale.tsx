import { Alert } from "@navikt/ds-react";
import { useNokkeltallForAvtale } from "../../../api/avtaler/useNokkeltallForAvtale";
import { Laster } from "../../../components/laster/Laster";
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
        subtitle="hittil i år"
        value={formaterTall(data.antallTiltaksgjennomforinger)}
        helptext="Sum av alle tiltaksgjennomføringer for valgt avtale, som er aktive innenfor budsjettåret (1. januar -> 31. desember)"
        helptextTitle="Hvor kommer tallene fra?"
      />
    </NokkeltallContainer>
  );
}
