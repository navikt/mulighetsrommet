import { Button } from "@navikt/ds-react";
import styles from "../../views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljer.module.scss";
import { CheckmarkIcon } from "@navikt/aksel-icons";
import { Delemodal } from "./delemodal/Delemodal";
import { formaterDato } from "../../utils/Utils";
import { useDelMedBruker } from "./delemodal/DelemodalReducer";
import { useLogEvent } from "../../logging/amplitude";
import { utledDelMedBrukerTekst } from "./delemodal/DelMedBrukerTekst";
import { useHentDeltMedBrukerStatus } from "../../core/api/queries/useHentDeltMedbrukerStatus";
import { useAppContext } from "../../hooks/useAppContext";
import { erBrukerResertMotElektroniskKommunikasjon } from "../../utils/Bruker";
import { Bruker, VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";

interface Props {
  knappetekst: string;
  veiledernavn: string;
  brukerdata: Bruker;
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

export const DelMedBruker = ({
  knappetekst,
  veiledernavn,
  brukerdata,
  tiltaksgjennomforing,
}: Props) => {
  const { fnr } = useAppContext();
  const { harDeltMedBruker } = useHentDeltMedBrukerStatus(fnr, tiltaksgjennomforing);

  const originaldeletekstFraTiltakstypen = tiltaksgjennomforing?.tiltakstype.delingMedBruker ?? "";

  const { logEvent } = useLogEvent();
  const { reservert } = erBrukerResertMotElektroniskKommunikasjon(brukerdata!);

  const deletekst = utledDelMedBrukerTekst(
    originaldeletekstFraTiltakstypen,
    tiltaksgjennomforing?.navn!,
    brukerdata.fornavn,
  );
  const [state, dispatch] = useDelMedBruker(deletekst);

  harDeltMedBruker && formaterDato(new Date(harDeltMedBruker.createdAt!!));

  const handleClickApneModal = () => {
    logEvent({
      name: "arbeidsmarkedstiltak.del-med-bruker",
      data: { action: "Åpnet delemodal", tiltakstype: tiltaksgjennomforing.tiltakstype.navn },
    });
    reservert
      ? dispatch({ type: "Toggle statusmodal", payload: true })
      : dispatch({ type: "Toggle modal", payload: true });
  };

  return (
    <>
      <Button
        onClick={handleClickApneModal}
        variant="secondary"
        className={styles.deleknapp}
        aria-label="Dele"
        data-testid="deleknapp"
        icon={harDeltMedBruker && <CheckmarkIcon title="Suksess" />}
        iconPosition="left"
      >
        {knappetekst}
      </Button>

      <Delemodal
        brukernavn={brukerdata.fornavn}
        veiledernavn={veiledernavn}
        tiltaksgjennomforing={tiltaksgjennomforing}
        harDeltMedBruker={harDeltMedBruker}
        dispatch={dispatch}
        state={state}
        brukerdata={brukerdata}
      />
    </>
  );
};
