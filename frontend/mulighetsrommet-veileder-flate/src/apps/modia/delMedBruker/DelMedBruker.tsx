import { Alert, Button } from "@navikt/ds-react";
import { CheckmarkIcon } from "@navikt/aksel-icons";
import { Delemodal } from "./Delemodal";
import { useDelMedBruker } from "./DelemodalReducer";
import { Brukerdata, DelMedBrukerDbo as DelMedBrukerInfo, VeilederflateTiltak } from "@api-client";
import { formaterDato } from "@/utils/Utils";
import {
  erBrukerReservertMotDigitalKommunikasjon,
  utledDelMedBrukerTekst,
} from "@/apps/modia/delMedBruker/helpers";

interface Props {
  veiledernavn: string;
  bruker: Brukerdata;
  tiltak: VeilederflateTiltak;
  delMedBrukerInfo?: DelMedBrukerInfo;
  veilederEnhet: string;
  veilederFylke?: string | null;
}

export function DelMedBruker({
  veiledernavn,
  bruker,
  tiltak,
  delMedBrukerInfo,
  veilederEnhet,
  veilederFylke,
}: Props) {
  const { reservert, melding } = erBrukerReservertMotDigitalKommunikasjon(bruker);

  const deletekst = utledDelMedBrukerTekst(tiltak, veiledernavn);
  const [state, dispatch] = useDelMedBruker(deletekst);

  const handleClickApneModal = () => {
    dispatch({ type: "Toggle modal", payload: true });
  };

  const knappetekst = delMedBrukerInfo?.createdAt
    ? `Delt i dialogen ${formaterDato(new Date(delMedBrukerInfo.createdAt))}`
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
            tiltak={tiltak}
            harDeltMedBruker={delMedBrukerInfo}
            dispatch={dispatch}
            state={state}
            bruker={bruker}
            veilederEnhet={veilederEnhet}
            veilederFylke={veilederFylke}
          />
        </>
      )}
    </>
  );
}
