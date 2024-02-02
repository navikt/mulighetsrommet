import { Button } from "@navikt/ds-react";
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
  brukerdata: Bruker;
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  delMedBrukerInfo?: DelMedBrukerInfo;

  lagreVeilederHarDeltTiltakMedBruker(
    dialogId: string,
    gjennomforing: VeilederflateTiltaksgjennomforing,
  ): Promise<void>;
}

export const DelMedBruker = ({
  veiledernavn,
  brukerdata,
  tiltaksgjennomforing,
  delMedBrukerInfo,
  lagreVeilederHarDeltTiltakMedBruker,
}: Props) => {
  const { logEvent } = useLogEvent();
  const { reservert } = erBrukerReservertMotElektroniskKommunikasjon(brukerdata);

  const deletekst = utledDelMedBrukerTekst(tiltaksgjennomforing, brukerdata.fornavn);
  const [state, dispatch] = useDelMedBruker(deletekst);

  const handleClickApneModal = () => {
    logEvent({
      name: "arbeidsmarkedstiltak.del-med-bruker",
      data: { action: "Åpnet delemodal", tiltakstype: tiltaksgjennomforing.tiltakstype.navn },
    });
    reservert
      ? dispatch({ type: "Toggle statusmodal", payload: true })
      : dispatch({ type: "Toggle modal", payload: true });
  };

  const knappetekst = delMedBrukerInfo?.createdAt
    ? `Delt med bruker ${formaterDato(new Date(delMedBrukerInfo.createdAt))}`
    : "Del med bruker";

  return (
    <>
      <Button
        onClick={handleClickApneModal}
        variant="secondary"
        aria-label="Dele"
        data-testid="deleknapp"
        icon={delMedBrukerInfo && <CheckmarkIcon title="Suksess" />}
        iconPosition="left"
      >
        {knappetekst}
      </Button>

      <Delemodal
        brukernavn={brukerdata.fornavn}
        veiledernavn={veiledernavn}
        tiltaksgjennomforing={tiltaksgjennomforing}
        harDeltMedBruker={delMedBrukerInfo}
        dispatch={dispatch}
        state={state}
        brukerdata={brukerdata}
        lagreVeilederHarDeltTiltakMedBruker={lagreVeilederHarDeltTiltakMedBruker}
      />
    </>
  );
};
