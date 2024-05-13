import { useNavTiltaksgjennomforingById } from "@/api/queries/useTiltaksgjennomforingById";
import { TiltakLoader } from "@/components/TiltakLoader";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { Alert } from "@navikt/ds-react";
import { InlineErrorBoundary } from "../../../ErrorBoundary";
import { PersonvernContainer } from "../../../components/personvern/PersonvernContainer";
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
        <>
          {data?.personvernBekreftet ? (
            <InlineErrorBoundary>
              <PersonvernContainer tiltaksgjennomforing={data} />
            </InlineErrorBoundary>
          ) : null}
          <LenkeListe
            lenker={data?.faneinnhold?.lenker?.filter((lenke) => !lenke.visKunForVeileder)}
          />
        </>
      }
    />
  );
}
