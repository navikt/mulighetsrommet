import { Alert, Loader } from "@navikt/ds-react";
import { useHentBrukerdata } from "../../core/api/queries/useHentBrukerdata";
import { useHentDeltMedBrukerStatus } from "../../core/api/queries/useHentDeltMedbrukerStatus";
import { useHentVeilederdata } from "../../core/api/queries/useHentVeilederdata";
import useTiltaksgjennomforingById from "../../core/api/queries/useTiltaksgjennomforingById";
import { useBrukerHarRettPaaTiltak } from "../../hooks/useBrukerHarRettPaaTiltak";
import { useFnr } from "../../hooks/useFnr";
import ViewTiltaksgjennomforingDetaljer from "./ViewTiltaksgjennomforingDetaljer";
import styles from "./ViewTiltaksgjennomforingDetaljer.module.scss";
import { useTitle } from "mulighetsrommet-frontend-common";

export function ViewTiltaksgjennomforingDetaljerContainer() {
  const { data: tiltaksgjennomforing, isLoading, isError } = useTiltaksgjennomforingById();
  useTitle(
    `Arbeidsmarkedstiltak - Detaljer ${
      tiltaksgjennomforing?.navn ? `- ${tiltaksgjennomforing.navn}` : null
    }`,
  );
  const fnr = useFnr();
  const { harDeltMedBruker } = useHentDeltMedBrukerStatus(fnr, tiltaksgjennomforing);
  const { brukerHarRettPaaTiltak, innsatsgruppeForGjennomforing } = useBrukerHarRettPaaTiltak();
  const veilederdata = useHentVeilederdata();
  const brukerdata = useHentBrukerdata();

  if (isLoading) {
    return (
      <div className={styles.filter_loader}>
        <Loader size="xlarge" />
      </div>
    );
  }

  if (isError) {
    return <Alert variant="error">Det har skjedd en feil</Alert>;
  }

  if (!tiltaksgjennomforing || !veilederdata?.data || !brukerdata?.data) return null;

  return (
    <ViewTiltaksgjennomforingDetaljer
      tiltaksgjennomforing={tiltaksgjennomforing}
      brukerHarRettPaaTiltak={brukerHarRettPaaTiltak}
      innsatsgruppeForGjennomforing={innsatsgruppeForGjennomforing}
      harDeltMedBruker={harDeltMedBruker}
      veilederdata={veilederdata.data}
      brukerdata={brukerdata.data}
    />
  );
}
