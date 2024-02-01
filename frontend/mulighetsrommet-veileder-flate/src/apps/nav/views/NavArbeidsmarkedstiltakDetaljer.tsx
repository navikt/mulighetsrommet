import { Alert } from "@navikt/ds-react";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { TiltakLoader } from "@/components/TiltakLoader";
import { useNavTiltaksgjennomforingById } from "@/core/api/queries/useTiltaksgjennomforingById";

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
      brukerActions={null}
    />
  );
}
