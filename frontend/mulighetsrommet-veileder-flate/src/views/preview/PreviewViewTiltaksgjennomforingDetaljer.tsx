import { Alert, Loader } from "@navikt/ds-react";
import { Innsatsgruppe, NavEnhetType } from "mulighetsrommet-api-client";
import usePreviewTiltaksgjennomforingById from "../../core/api/queries/usePreviewTiltaksgjennomforingById";
import ViewTiltaksgjennomforingDetaljer from "../ViewTiltaksgjennomforingDetaljer/ViewTiltaksgjennomforingDetaljer";
import styles from "./PreviewView.module.scss";
import Tilbakeknapp from "../../components/tilbakeknapp/Tilbakeknapp";
import { DelMedBruker } from "../../components/delMedBruker/DelMedBruker";

export function PreviewViewTiltaksgjennomforingDetaljer() {
  const { data, isLoading, isError } = usePreviewTiltaksgjennomforingById();
  const brukersInnsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS;

  if (isLoading) {
    return (
      <div className={styles.filter_loader}>
        <Loader size="xlarge" title="Laster tiltaksgjennomføring..." />
      </div>
    );
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!data) return <Alert variant="error">Klarte ikke finne tiltaksgjennomføringen</Alert>;

  return (
    <>
      <Alert style={{ marginBottom: "2rem" }} variant="warning" data-testid="sanity-preview-alert">
        Forhåndsvisning av informasjon
      </Alert>
      <ViewTiltaksgjennomforingDetaljer
        tiltaksgjennomforing={data}
        brukersInnsatsgruppe={brukersInnsatsgruppe}
        knapperad={<Tilbakeknapp tilbakelenke="/preview" tekst="Tilbake til tiltaksoversikten" />}
        brukerActions={
          <div>
            <DelMedBruker
              tiltaksgjennomforing={data}
              knappetekst="Del med bruker"
              veiledernavn="{Veiledernavn}"
              brukerdata={{
                fnr: "12345678910",
                fornavn: "{NAVN}",
                manuellStatus: {
                  erUnderManuellOppfolging: false,
                  krrStatus: { kanVarsles: true, erReservert: false },
                },
                varsler: [],
                enheter: [
                  {
                    navn: "{GEOGRAFISK_ENHET}",
                    enhetsnummer: "0",
                    overordnetEnhet: "0100",
                    type: NavEnhetType.LOKAL,
                  },
                ],
              }}
            />
          </div>
        }
      />
    </>
  );
}
