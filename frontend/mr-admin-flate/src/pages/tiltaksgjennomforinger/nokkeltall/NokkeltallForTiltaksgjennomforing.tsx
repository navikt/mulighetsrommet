import { Nokkeltall } from "../../../components/nokkeltall/Nokkeltall";
import { Laster } from "../../../components/laster/Laster";
import { formaterTall } from "../../../utils/Utils";
import { Alert } from "@navikt/ds-react";
import { NokkeltallContainer } from "../../../components/nokkeltall/NokkeltallContainer";
import { useNokkeltallForTiltaksgjennomforing } from "../../../api/tiltaksgjennomforing/useNokkeltallForTiltaksgjennomforing";
import { NokkeltallAlert } from "../../../components/nokkeltall/NokkeltallAlert";

export function NokkeltallForTiltaksgjennomforing() {
  const { data, isLoading } = useNokkeltallForTiltaksgjennomforing();

  if (isLoading && !data) {
    return <Laster tekst={"Henter nøkkeltall..."} />;
  }

  if (!data) {
    return <Alert variant={"error"}>Fant ingen nøkkeltall</Alert>;
  }

  return (
    <>
      <NokkeltallAlert />
      <NokkeltallContainer>
        <Nokkeltall
          title="Deltakere"
          subtitle="hittil i år"
          value={formaterTall(data.antallDeltakere)}
          helptext="Sum av alle deltakere for valgt tiltaksgjennomføring, som er aktive innenfor budsjettåret (1. januar -> 31. desember)"
          helptextTitle="Hvor kommer tallene fra?"
        />
      </NokkeltallContainer>
    </>
  );
}
