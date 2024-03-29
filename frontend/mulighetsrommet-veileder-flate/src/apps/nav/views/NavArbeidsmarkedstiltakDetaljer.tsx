import { Alert } from "@navikt/ds-react";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { TiltakLoader } from "@/components/TiltakLoader";
import { useNavTiltaksgjennomforingById } from "@/api/queries/useTiltaksgjennomforingById";
import { LenkeListe } from "../../../components/sidemeny/Lenker";

export function NavArbeidsmarkedstiltakDetaljer() {
  const { data, isLoading, isError } = useNavTiltaksgjennomforingById();

  if (isLoading) {
    return <TiltakLoader />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!data) return <Alert variant="error">Klarte ikke finne tiltaksgjennomføringen</Alert>;

  return (
    <ViewTiltaksgjennomforingDetaljer
      tiltaksgjennomforing={data}
      knapperad={<Tilbakeknapp tilbakelenke=".." tekst="Tilbake til tiltaksoversikten" />}
      brukerActions={
        <LenkeListe
          lenker={data?.faneinnhold?.lenker?.filter((lenke) => !lenke.visKunForVeileder)}
        />
      }
    />
  );
}
