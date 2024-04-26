import { usePreviewTiltaksgjennomforingById } from "@/api/queries/useTiltaksgjennomforingById";
import { DelMedBruker } from "@/apps/modia/delMedBruker/DelMedBruker";
import { TiltakLoader } from "@/components/TiltakLoader";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { Alert } from "@navikt/ds-react";
import { Innsatsgruppe, NavEnhetStatus, NavEnhetType } from "mulighetsrommet-api-client";
import { InlineErrorBoundary } from "../../../ErrorBoundary";
import { PersonvernContainer } from "../../../components/personvern/PersonvernContainer";
import { LenkeListe } from "../../../components/sidemeny/Lenker";

export function PreviewArbeidsmarkedstiltakDetaljer() {
  const { data, isPending, isError } = usePreviewTiltaksgjennomforingById();
  const brukersInnsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS;

  if (isPending) {
    return <TiltakLoader />;
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  return (
    <>
      <Alert style={{ marginBottom: "2rem" }} variant="warning">
        Forhåndsvisning av informasjon
      </Alert>
      <ViewTiltaksgjennomforingDetaljer
        tiltaksgjennomforing={data}
        brukersInnsatsgruppe={brukersInnsatsgruppe}
        knapperad={<Tilbakeknapp tilbakelenke=".." tekst="Tilbake til tiltaksoversikten" />}
        brukerActions={
          <>
            <DelMedBruker
              tiltaksgjennomforing={data}
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
              lagreVeilederHarDeltTiltakMedBruker={async (dialogId, gjennomforing) => {
                // eslint-disable-next-line no-console
                console.log("Del med bruker", dialogId, gjennomforing);
              }}
            />
            {data && data?.personvernBekreftet ? (
              <InlineErrorBoundary>
                <PersonvernContainer tiltaksgjennomforing={data} />
              </InlineErrorBoundary>
            ) : null}
            <LenkeListe lenker={data?.faneinnhold?.lenker} />
          </>
        }
      />
    </>
  );
}
