import { ModiaRoute, navigateToModiaApp } from "@/apps/modia/ModiaRoute";
import { PortenLink } from "@/components/PortenLink";
import { StatusModal } from "@/components/modal/StatusModal";
import { useLogEvent } from "@/logging/amplitude";
import { erPreview } from "@/utils/Utils";
import { BodyShort, Button, Checkbox, Heading, HelpText, Modal } from "@navikt/ds-react";
import {
  Bruker,
  DelMedBruker,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { useDelTiltakViaDialogen } from "../../../api/queries/useDelTiltakViaDialogen";
import { DelMedBrukerContent, MAKS_ANTALL_TEGN_DEL_MED_BRUKER } from "./DelMedBrukerContent";
import style from "./Delemodal.module.scss";
import { Actions, State } from "./DelemodalActions";

interface DelemodalProps {
  veiledernavn?: string;
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  bruker: Bruker;
  harDeltMedBruker?: DelMedBruker;
  dispatch: (action: Actions) => void;
  state: State;
}

export function Delemodal({
  veiledernavn,
  tiltaksgjennomforing,
  bruker,
  harDeltMedBruker,
  dispatch,
  state,
}: DelemodalProps) {
  const { logEvent } = useLogEvent();
  const mutation = useDelTiltakViaDialogen({
    onSuccess: (response) => {
      dispatch({ type: "Sendt ok", payload: response.id });
      mutation.reset();
    },
  });

  const senderTilDialogen = state.sendtStatus === "SENDER";
  const { enableRedigerDeletekst } = state;

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
      mutation.mutate({
        fnr: bruker.fnr,
        overskrift,
        tekst,
        venterPaaSvarFraBruker,
        tiltaksgjennomforingId: tiltaksgjennomforing?.id || null,
        sanityId: tiltaksgjennomforing?.sanityId || null,
      });
    } catch {
      dispatch({ type: "Sending feilet" });
      logDelMedbrukerEvent("Del med bruker feilet", tiltaksgjennomforing.tiltakstype.navn);
    }
  };

  const enableEndreDeletekst = () => {
    dispatch({ type: "Enable rediger deletekst", payload: true });
    logEvent({
      name: "arbeidsmarkedstiltak.del-med-bruker",
      data: { action: "Endre deletekst", tiltakstype: tiltaksgjennomforing.tiltakstype.navn },
    });
  };

  return (
    <>
      <Modal
        open={state.modalOpen}
        onClose={lukkModal}
        className={style.delemodal}
        aria-label="modal"
      >
        <Modal.Header closeButton>
          <Heading size="xsmall">Del med bruker</Heading>
          <Heading size="large" level="1" className={style.heading}>
            {"Tiltak gjennom NAV: " + tiltaksgjennomforing.navn}
          </Heading>
        </Modal.Header>

        <Modal.Body className={style.body}>
          <DelMedBrukerContent
            state={state}
            dispatch={dispatch}
            veiledernavn={veiledernavn}
            brukernavn={bruker.fornavn}
            harDeltMedBruker={harDeltMedBruker}
            tiltaksgjennomforing={tiltaksgjennomforing}
            enableRedigerDeletekst={enableRedigerDeletekst}
          />
        </Modal.Body>

        <Modal.Footer className={style.delemodal_footer}>
          <div className={style.delemodal_actions}>
            {enableRedigerDeletekst ? null : (
              <Button size="small" onClick={enableEndreDeletekst} variant="secondary">
                Rediger melding
              </Button>
            )}
            <div className={style.delemodal_venter_pa_svar}>
              <Checkbox
                onChange={(e) => {
                  dispatch({
                    type: "Venter på svar fra bruker",
                    payload: e.currentTarget.checked,
                  });
                  if (e.currentTarget.checked) {
                    logEvent({
                      name: "arbeidsmarkedstiltak.del-med-bruker",
                      data: {
                        action: "Sett venter på svar fra bruker",
                        tiltakstype: tiltaksgjennomforing.tiltakstype.navn,
                      },
                    });
                  }
                }}
                checked={state.venterPaaSvarFraBruker}
                value="venter-pa-svar-fra-bruker"
              >
                Venter på svar fra bruker
              </Checkbox>
              <HelpText title="Hva betyr dette valget?">
                Ved å huke av for at du venter på svar fra bruker vil du kunne bruke filteret i
                oversikten til å se alle brukere du venter på svar fra.
              </HelpText>
            </div>
          </div>

          <BodyShort size="small">
            Kandidatene vil få et varsel fra NAV, og kan logge inn på nav.no for å lese meldingen.
          </BodyShort>
          <div className={style.hr} />
          <div className={style.knapperad}>
            <Button
              size="small"
              variant="tertiary"
              onClick={clickCancel}
              data-testid="modal_btn-cancel"
              disabled={senderTilDialogen}
            >
              Avbryt
            </Button>
            <Button
              size="small"
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
        </Modal.Footer>
      </Modal>

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
          primaryButtonOnClick={(event) => {
            event.preventDefault();
            navigateToModiaApp({
              route: ModiaRoute.DIALOG,
              dialogId: state.dialogId,
            });
          }}
          secondaryButtonText="Lukk"
          secondaryButtonOnClick={lukkStatusmodal}
        />
      )}
    </>
  );
}
