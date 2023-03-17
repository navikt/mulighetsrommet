import { Nokkeltall } from "../../../components/nokkeltall/Nokkeltall";
import { useNokkeltallForTiltakstype } from "../../../api/tiltakstyper/useNokkeltallForTiltakstype";
import { Laster } from "../../../components/laster/Laster";
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
        subtitle="hittil i år"
        value={formaterTall(data.antallAvtaler)}
        helptext="Sum av alle avtaler for valgt tiltakstype, som er aktive innenfor budsjettåret (1. januar -> 31. desember)"
        helptextTitle="Hvor kommer tallene fra?"
      />
      <Nokkeltall
        title="Gjennomføringer"
        subtitle="hittil i år"
        value={formaterTall(data.antallTiltaksgjennomforinger)}
        helptext="Sum av alle tiltaksgjennomføringer for valgt tiltakstype, som er aktive innenfor budsjettåret (1. januar -> 31. desember)"
        helptextTitle="Hvor kommer tallene fra?"
      />
      <Nokkeltall
        title="Deltakere"
        subtitle="hittil i år"
        value={formaterTall(data.antallDeltakere)}
        helptext="Sum av alle deltakere for valgt tiltakstype, som er aktive innenfor budsjettåret (1. januar -> 31. desember)"
        helptextTitle="Hvor kommer tallene fra?"
      />
    </NokkeltallContainer>
  );
}
