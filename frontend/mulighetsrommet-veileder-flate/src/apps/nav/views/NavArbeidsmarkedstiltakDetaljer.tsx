import {
  isTiltakGruppe,
  useNavTiltaksgjennomforingById,
} from "@/api/queries/useTiltaksgjennomforingById";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { Alert } from "@navikt/ds-react";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { LenkeListe } from "@/components/sidemeny/Lenker";
import { DetaljerSkeleton } from "@mr/frontend-common";

export function NavArbeidsmarkedstiltakDetaljer() {
  const { data: tiltak, isError, isPending } = useNavTiltaksgjennomforingById();

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (isPending) {
    return <DetaljerSkeleton />;
  }

  if (!tiltak) {
    return <Alert variant="error">Klarte ikke finne tiltaksgjennomføringen</Alert>;
  }

  return (
    <ViewTiltaksgjennomforingDetaljer
      tiltak={tiltak}
      knapperad={<Tilbakeknapp tilbakelenke=".." tekst="Gå til oversikt over aktuelle tiltak" />}
      brukerActions={
        <>
          {isTiltakGruppe(tiltak) && tiltak.personvernBekreftet ? (
            <InlineErrorBoundary>
              <PersonvernContainer tiltaksgjennomforing={tiltak} />
            </InlineErrorBoundary>
          ) : null}
          <LenkeListe
            lenker={tiltak.faneinnhold?.lenker?.filter((lenke) => !lenke.visKunForVeileder)}
          />
        </>
      }
    />
  );
}
