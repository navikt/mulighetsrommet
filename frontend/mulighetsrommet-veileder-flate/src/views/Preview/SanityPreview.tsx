import { Alert, Loader } from "@navikt/ds-react";
import { Innsatsgruppe, NavEnhetType } from "mulighetsrommet-api-client";
import usePreviewTiltaksgjennomforingById from "../../core/api/queries/usePreviewTiltaksgjennomforingById";
import ViewTiltaksgjennomforingDetaljer from "../tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljer";
import styles from "./SanityPreview.module.scss";
import { Link } from "react-router-dom";

export function SanityPreview() {
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
      <Link to="/preview">Tilbake</Link>
      <ViewTiltaksgjennomforingDetaljer
        tiltaksgjennomforing={data}
        brukerHarRettPaaTiltak={true}
        brukersInnsatsgruppe={brukersInnsatsgruppe}
        innsatsgruppeForGjennomforing={Innsatsgruppe.VARIG_TILPASSET_INNSATS}
        harDeltMedBruker={undefined}
        veilederdata={{
          etternavn: "Veiledersen",
          fornavn: "Veileder",
          hovedenhet: { enhetsnummer: "0519", navn: "Hovedenhet veileder" },
          navIdent: "V123456",
        }}
        brukerdata={{
          fnr: "99999999999",
          fornavn: "Forhånds",
          innsatsgruppe: brukersInnsatsgruppe,
          geografiskEnhet: {
            enhetsnummer: "1234",
            navn: "Forhåndsvisningsenhet",
            type: NavEnhetType.LOKAL,
            overordnetEnhet: null,
          },
          oppfolgingsenhet: {
            enhetsnummer: "1234",
            navn: "Oppfølgingsenhet",
            type: NavEnhetType.LOKAL,
            overordnetEnhet: null,
          },
          manuellStatus: {
            erUnderManuellOppfolging: false,
            krrStatus: {
              erReservert: false,
              kanVarsles: true,
            },
          },
        }}
      />
    </>
  );
}
