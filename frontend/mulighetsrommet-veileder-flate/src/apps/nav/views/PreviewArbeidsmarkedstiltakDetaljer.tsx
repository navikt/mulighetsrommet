import {
  isTiltakGruppe,
  usePreviewTiltaksgjennomforingById,
} from "@/api/queries/useTiltaksgjennomforingById";
import { DelMedBruker } from "@/apps/modia/delMedBruker/DelMedBruker";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { Alert } from "@navikt/ds-react";
import { Innsatsgruppe, NavEnhetStatus, NavEnhetType } from "@mr/api-client";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { LenkeListe } from "@/components/sidemeny/Lenker";
import { DetaljerSkeleton } from "@mr/frontend-common";

export function PreviewArbeidsmarkedstiltakDetaljer() {
  const { data: tiltak, isPending, isError } = usePreviewTiltaksgjennomforingById();

  if (isPending) {
    return <DetaljerSkeleton />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!tiltak) {
    return <Alert variant="error">Klarte ikke finne tiltaksgjennomføringen</Alert>;
  }

  return (
    <>
      <Alert style={{ marginBottom: "2rem" }} variant="warning">
        Forhåndsvisning av informasjon
      </Alert>
      <ViewTiltaksgjennomforingDetaljer
        tiltak={tiltak}
        knapperad={<Tilbakeknapp tilbakelenke=".." tekst="Gå til oversikt over aktuelle tiltak" />}
        brukerActions={
          <>
            <DelMedBruker
              tiltak={tiltak}
              veiledernavn="{Veiledernavn}"
              bruker={{
                innsatsgruppe: Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                fnr: "12345678910",
                fornavn: "{NAVN}",
                manuellStatus: {
                  erUnderManuellOppfolging: false,
                  krrStatus: { kanVarsles: true, erReservert: false },
                },
                erUnderOppfolging: true,
                varsler: [],
                enheter: [
                  {
                    navn: "{GEOGRAFISK_ENHET}",
                    enhetsnummer: "0",
                    overordnetEnhet: "0100",
                    type: NavEnhetType.LOKAL,
                    status: NavEnhetStatus.AKTIV,
                  },
                ],
              }}
            />
            {isTiltakGruppe(tiltak) && tiltak.personvernBekreftet ? (
              <InlineErrorBoundary>
                <PersonvernContainer tiltaksgjennomforing={tiltak} />
              </InlineErrorBoundary>
            ) : null}
            <LenkeListe lenker={tiltak.faneinnhold?.lenker} />
          </>
        }
      />
    </>
  );
}
