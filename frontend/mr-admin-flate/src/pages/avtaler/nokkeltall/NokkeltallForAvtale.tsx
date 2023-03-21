import { Alert, BodyShort } from "@navikt/ds-react";
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
    <>
      <Alert style={{ marginBottom: "1rem" }} variant="warning">
        <BodyShort>
          Tjenesten er under utvikling og tallene som vises her under nøkkeltall
          kan være feil eller misvisende pga. feil eller for dårlig datagrunnlag
        </BodyShort>
      </Alert>
      <NokkeltallContainer>
        <Nokkeltall
          title="Tiltaksgjennomføringer"
          subtitle="hittil i år"
          value={formaterTall(data.antallTiltaksgjennomforinger)}
          helptext="Sum av alle tiltaksgjennomføringer for valgt avtale, som er aktive innenfor budsjettåret (1. januar -> 31. desember)"
          helptextTitle="Hvor kommer tallene fra?"
        />
      </NokkeltallContainer>
    </>
  );
}
