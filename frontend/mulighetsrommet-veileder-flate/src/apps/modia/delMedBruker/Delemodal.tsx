import { BodyShort, Button, Checkbox, Heading, HelpText, HStack, Modal } from "@navikt/ds-react";
import {
  Bruker,
  DelMedBruker,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { MODIA_PORTEN } from "mulighetsrommet-frontend-common/constants";
import { mulighetsrommetClient } from "@/core/api/clients";
import { useLogEvent } from "@/logging/amplitude";
import { byttTilDialogFlate } from "@/utils/DialogFlateUtils";
import { erPreview } from "@/utils/Utils";
import { StatusModal } from "@/components/modal/StatusModal";
import { DelMedBrukerContent, MAKS_ANTALL_TEGN_DEL_MED_BRUKER } from "./DelMedBrukerContent";
import delemodalStyles from "./Delemodal.module.scss";
import { Actions, State } from "./DelemodalActions";
import { erBrukerReservertMotElektroniskKommunikasjon } from "@/apps/modia/delMedBruker/helpers";

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

            <HStack gap="1" style={{ marginTop: "1rem" }}>
              <Checkbox
                onChange={(e) => {
                  dispatch({
                    type: "Venter på svar fra bruker",
                    payload: e.currentTarget.checked,
                  });
                  if (e.currentTarget.checked) {
                    logDelMedbrukerEvent(
                      "Sett venter på svar fra bruker",
                      tiltaksgjennomforing.tiltakstype.navn,
                    );
                  }
                }}
                checked={state.venterPaaSvarFraBruker}
                value="venter-pa-svar-fra-bruker"
                data-testid="venter-pa-svar_checkbox"
              >
                Venter på svar fra bruker
              </Checkbox>
              <HelpText title="Hva betyr dette valget?">
                Ved å huke av for at du venter på svar fra bruker vil du kunne bruke filteret i
                oversikten til å se alle brukere du venter på svar fra.
              </HelpText>
            </HStack>
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
                data-testid="modal_btn-send"
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
              <a href={MODIA_PORTEN}>kontakt i Porten</a> dersom du trenger mer hjelp.
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
