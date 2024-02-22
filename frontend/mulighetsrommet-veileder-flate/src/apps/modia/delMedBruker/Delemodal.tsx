import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
import {
  Bruker,
  DelMedBruker,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { mulighetsrommetClient } from "@/core/api/clients";
import { useLogEvent } from "@/logging/amplitude";
import { byttTilDialogFlate } from "@/utils/DialogFlateUtils";
import { erPreview } from "@/utils/Utils";
import { StatusModal } from "@/components/modal/StatusModal";
import { DelMedBrukerContent, MAKS_ANTALL_TEGN_DEL_MED_BRUKER } from "./DelMedBrukerContent";
import delemodalStyles from "./Delemodal.module.scss";
import { Actions, State } from "./DelemodalActions";
import { erBrukerReservertMotElektroniskKommunikasjon } from "@/apps/modia/delMedBruker/helpers";
import { PortenLink } from "@/components/PortenLink";

interface DelemodalProps {
  brukernavn?: string;
  veiledernavn?: string;
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  brukerdata: Bruker;
  harDeltMedBruker?: DelMedBruker;
  dispatch: (action: Actions) => void;
  state: State;

  lagreVeilederHarDeltTiltakMedBruker(
    dialogId: string,
    gjennomforing: VeilederflateTiltaksgjennomforing,
  ): Promise<void>;
}

export function Delemodal({
  brukernavn,
  veiledernavn,
  tiltaksgjennomforing,
  brukerdata,
  harDeltMedBruker,
  dispatch,
  state,
  lagreVeilederHarDeltTiltakMedBruker,
}: DelemodalProps) {
  const { logEvent } = useLogEvent();

  const senderTilDialogen = state.sendtStatus === "SENDER";

  const originaltekstLengde = state.originalDeletekst.length;
  const lukkStatusmodal = () => dispatch({ type: "Toggle statusmodal", payload: false });
  const lukkModal = () => dispatch({ type: "Toggle modal", payload: false });
  const logDelMedbrukerEvent = (
    action:
      | "Delte med bruker"
      | "Del med bruker feilet"
      | "Avbrutt del med bruker"
      | "Sett venter på svar fra bruker",
    tiltakstype: string,
  ) => {
    logEvent({
      name: "arbeidsmarkedstiltak.del-med-bruker",
      data: { action, tiltakstype },
    });
  };

  const clickCancel = () => {
    lukkModal();
    dispatch({ type: "Avbryt" });
    logDelMedbrukerEvent("Avbrutt del med bruker", tiltaksgjennomforing.tiltakstype.navn);
  };

  const handleSend = async () => {
    const { deletekst, venterPaaSvarFraBruker } = state;
    if (deletekst.trim().length > state.originalDeletekst.length + 500) {
      return;
    }

    logDelMedbrukerEvent("Delte med bruker", tiltaksgjennomforing.tiltakstype.navn);

    dispatch({ type: "Send melding" });
    const overskrift = `Tiltak gjennom NAV: ${tiltaksgjennomforing.navn}`;
    const tekst = state.deletekst;
    try {
      const res = await mulighetsrommetClient.dialogen.delMedDialogen({
        requestBody: {
          norskIdent: brukerdata?.fnr,
          overskrift,
          tekst,
          venterPaaSvarFraBruker,
        },
      });
      await lagreVeilederHarDeltTiltakMedBruker(res.id, tiltaksgjennomforing);
      dispatch({ type: "Sendt ok", payload: res.id });
    } catch {
      dispatch({ type: "Sending feilet" });
      logDelMedbrukerEvent("Del med bruker feilet", tiltaksgjennomforing.tiltakstype.navn);
    }
  };

  const { reservert, melding } = erBrukerReservertMotElektroniskKommunikasjon(brukerdata);

  return (
    <>
      {reservert ? (
        <StatusModal
          modalOpen={state.statusmodalOpen}
          onClose={lukkStatusmodal}
          ikonVariant="warning"
          heading="Kunne ikke dele tiltaket"
          text={melding}
          primaryButtonText="OK"
          primaryButtonOnClick={lukkStatusmodal}
        />
      ) : (
        <Modal
          open={state.modalOpen}
          onClose={lukkModal}
          className={delemodalStyles.delemodal}
          aria-label="modal"
        >
          <Modal.Header closeButton>
            <Heading size="xsmall">Del med bruker</Heading>
            <Heading size="large" level="1" className={delemodalStyles.heading}>
              {"Tiltak gjennom NAV: " + tiltaksgjennomforing.navn}
            </Heading>
          </Modal.Header>
          <Modal.Body className={delemodalStyles.body}>
            <DelMedBrukerContent
              state={state}
              dispatch={dispatch}
              veiledernavn={veiledernavn}
              brukernavn={brukernavn}
              harDeltMedBruker={harDeltMedBruker}
              tiltaksgjennomforing={tiltaksgjennomforing}
            />
          </Modal.Body>
          <Modal.Footer>
            <div className={delemodalStyles.knapperad}>
              <Button
                variant="tertiary"
                onClick={clickCancel}
                data-testid="modal_btn-cancel"
                disabled={senderTilDialogen}
              >
                Avbryt
              </Button>
              <Button
                onClick={handleSend}
                disabled={
                  senderTilDialogen ||
                  state.deletekst.length === 0 ||
                  state.deletekst.length > originaltekstLengde + MAKS_ANTALL_TEGN_DEL_MED_BRUKER ||
                  erPreview()
                }
              >
                {senderTilDialogen ? "Sender..." : "Send via Dialogen"}
              </Button>
            </div>
            <BodyShort size="small">
              Kandidatene vil få et varsel fra NAV, og kan logge inn på nav.no for å lese meldingen.
            </BodyShort>
          </Modal.Footer>
        </Modal>
      )}

      {state.sendtStatus === "SENDING_FEILET" && (
        <StatusModal
          modalOpen={state.statusmodalOpen}
          ikonVariant="error"
          heading="Tiltaket kunne ikke deles"
          text={
            <>
              Tiltaket kunne ikke deles på grunn av en teknisk feil hos oss. Forsøk på nytt eller ta{" "}
              <PortenLink>kontakt i Porten</PortenLink>
              dersom du trenger mer hjelp.
            </>
          }
          onClose={lukkStatusmodal}
          primaryButtonOnClick={() => dispatch({ type: "Reset" })}
          primaryButtonText="Prøv igjen"
          secondaryButtonOnClick={lukkStatusmodal}
          secondaryButtonText="Avbryt"
        />
      )}

      {state.sendtStatus === "SENDT_OK" && (
        <StatusModal
          modalOpen={state.statusmodalOpen}
          onClose={lukkStatusmodal}
          ikonVariant="success"
          heading="Tiltaket er delt med brukeren"
          text="Det er opprettet en ny tråd i Dialogen der du kan fortsette kommunikasjonen rundt dette tiltaket med brukeren."
          primaryButtonText="Gå til dialogen"
          primaryButtonOnClick={(event) => byttTilDialogFlate({ event, dialogId: state.dialogId })}
          secondaryButtonText="Lukk"
          secondaryButtonOnClick={lukkStatusmodal}
        />
      )}
    </>
  );
}
