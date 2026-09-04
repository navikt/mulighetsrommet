import { Alert, Button } from "@navikt/ds-react";
import { CheckmarkIcon } from "@navikt/aksel-icons";
import { Delemodal } from "./Delemodal";
import { useDelMedBruker } from "./DelemodalReducer";
import { Brukerdata, DelMedBrukerDto, VeilederflateTiltak } from "@arbeidsmarkedstiltak/api-client";
import { formaterDato } from "@/utils/Utils";
import {
  erBrukerReservertMotDigitalKommunikasjon,
  utledDelMedBrukerTekst,
} from "@/apps/modia/delMedBruker/helpers";

interface Props {
  veiledernavn: string;
  bruker: Brukerdata;
  tiltak: VeilederflateTiltak;
  delMedBruker?: DelMedBrukerDto;
  veilederEnhet: string;
}

export function DelMedBruker({ veiledernavn, bruker, tiltak, delMedBruker, veilederEnhet }: Props) {
  const { reservert, melding } = erBrukerReservertMotDigitalKommunikasjon(bruker);

  const deletekst = utledDelMedBrukerTekst(tiltak, veiledernavn);
  const [state, dispatch] = useDelMedBruker(deletekst);

  const handleClickApneModal = () => {
    dispatch({ type: "Toggle modal", payload: true });
  };

  const knappetekst = delMedBruker
    ? `Delt i dialogen ${formaterDato(new Date(delMedBruker.tidspunkt))}`
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
            icon={delMedBruker && <CheckmarkIcon title="Suksess" />}
            iconPosition="left"
          >
            {knappetekst}
          </Button>
          <Delemodal
            veiledernavn={veiledernavn}
            tiltak={tiltak}
            delMedBruker={delMedBruker}
            dispatch={dispatch}
            state={state}
            bruker={bruker}
            veilederEnhet={veilederEnhet}
          />
        </>
      )}
    </>
  );
}
