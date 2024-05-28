import { Alert, Button } from "@navikt/ds-react";
import { CheckmarkIcon } from "@navikt/aksel-icons";
import { Delemodal } from "./Delemodal";
import { useDelMedBruker } from "./DelemodalReducer";
import { useLogEvent } from "@/logging/amplitude";
import {
  Bruker,
  DelMedBruker as DelMedBrukerInfo,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { formaterDato } from "@/utils/Utils";
import {
  erBrukerReservertMotElektroniskKommunikasjon,
  utledDelMedBrukerTekst,
} from "@/apps/modia/delMedBruker/helpers";

interface Props {
  veiledernavn: string;
  bruker: Bruker;
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  delMedBrukerInfo?: DelMedBrukerInfo;

  lagreVeilederHarDeltTiltakMedBruker(
    dialogId: string,
    gjennomforing: VeilederflateTiltaksgjennomforing,
  ): Promise<void>;
}

export const DelMedBruker = ({
  veiledernavn,
  bruker,
  tiltaksgjennomforing,
  delMedBrukerInfo,
  lagreVeilederHarDeltTiltakMedBruker,
}: Props) => {
  const { logEvent } = useLogEvent();
  const { reservert, melding } = erBrukerReservertMotElektroniskKommunikasjon(bruker);

  const deletekst = utledDelMedBrukerTekst(tiltaksgjennomforing, veiledernavn);
  const [state, dispatch] = useDelMedBruker(deletekst);

  const handleClickApneModal = () => {
    logEvent({
      name: "arbeidsmarkedstiltak.del-med-bruker",
      data: { action: "Åpnet delemodal", tiltakstype: tiltaksgjennomforing.tiltakstype.navn },
    });
    dispatch({ type: "Toggle modal", payload: true });
  };

  const knappetekst = delMedBrukerInfo?.createdAt
    ? `Delt med bruker ${formaterDato(new Date(delMedBrukerInfo.createdAt))}`
    : "Del med bruker";

  return (
    <>
      {reservert ? (
        <Alert variant="warning">{melding}</Alert>
      ) : (
        <>
          <Button
            onClick={handleClickApneModal}
            variant="secondary"
            aria-label="Del med bruker"
            data-testid="deleknapp"
            icon={delMedBrukerInfo && <CheckmarkIcon title="Suksess" />}
            iconPosition="left"
          >
            {knappetekst}
          </Button>
          <Delemodal
            veiledernavn={veiledernavn}
            tiltaksgjennomforing={tiltaksgjennomforing}
            harDeltMedBruker={delMedBrukerInfo}
            dispatch={dispatch}
            state={state}
            bruker={bruker}
            lagreVeilederHarDeltTiltakMedBruker={lagreVeilederHarDeltTiltakMedBruker}
          />
        </>
      )}
    </>
  );
};
