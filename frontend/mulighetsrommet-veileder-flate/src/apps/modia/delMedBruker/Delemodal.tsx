import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";
import { useDelTiltakMedBruker } from "@/api/queries/useDelTiltakMedBruker";
import { ModiaRoute, navigateToModiaApp } from "@/apps/modia/ModiaRoute";
import { PortenLink } from "@/components/PortenLink";
import { StatusModal } from "@/components/modal/StatusModal";
import { useLogEvent } from "@/logging/amplitude";
import { Separator } from "@/utils/Separator";
import { erPreview } from "@/utils/Utils";
import { Bruker, DelMedBruker, VeilederflateTiltak } from "@mr/api-client-v2";
import { BodyShort, Button, Checkbox, Heading, HelpText, HStack, Modal } from "@navikt/ds-react";
import { DelMedBrukerContent, MAKS_ANTALL_TEGN_DEL_MED_BRUKER } from "./DelMedBrukerContent";
import { Actions, State } from "./DelemodalActions";

interface DelemodalProps {
  veiledernavn?: string;
  tiltak: VeilederflateTiltak;
  bruker: Bruker;
  harDeltMedBruker?: DelMedBruker;
  dispatch: (action: Actions) => void;
  state: State;
  veilederEnhet: string;
  veilederFylke?: string | null;
}

function overskrift(tiltak: VeilederflateTiltak): string {
  return `Tiltak gjennom Nav: ${tiltak.tiltakstype.navn}`;
}

export function Delemodal({
  veiledernavn,
  tiltak,
  bruker,
  harDeltMedBruker,
  dispatch,
  state,
  veilederEnhet,
  veilederFylke,
}: DelemodalProps) {
  const { logEvent } = useLogEvent();
  const mutation = useDelTiltakMedBruker({
    onSuccess: (response) => {
      dispatch({ type: "Sendt ok", payload: response.dialogId });
      logDelMedbrukerEvent("Delte med bruker", tiltak.tiltakstype.navn);
      mutation.reset();
    },
    onError: () => {
      dispatch({ type: "Sending feilet" });
      logDelMedbrukerEvent("Del med bruker feilet", tiltak.tiltakstype.navn);
    },
  });

  const senderTilDialogen = state.sendtStatus === "SENDER";
  const { enableRedigerDeletekst, sendtStatus, dialogId } = state;

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
    logDelMedbrukerEvent("Avbrutt del med bruker", tiltak.tiltakstype.navn);
  };

  const handleSend = async () => {
    const { deletekst, venterPaaSvarFraBruker } = state;
    if (deletekst.trim().length > state.originalDeletekst.length + 500) {
      return;
    }

    dispatch({ type: "Send melding" });
    const tekst = state.deletekst;
    mutation.mutate({
      fnr: bruker.fnr,
      overskrift: overskrift(tiltak),
      tekst,
      venterPaaSvarFraBruker,
      gjennomforingId: isTiltakGruppe(tiltak) ? tiltak.id : null,
      sanityId: !isTiltakGruppe(tiltak) ? tiltak.sanityId : null,
      tiltakstypeNavn: tiltak.tiltakstype.navn,
      veilederTilhorerFylke: veilederFylke || null,
      veilederTilhorerEnhet: veilederEnhet || null,
    });
  };

  const enableEndreDeletekst = () => {
    dispatch({ type: "Enable rediger deletekst", payload: true });
    logEvent({
      name: "arbeidsmarkedstiltak.del-med-bruker",
      data: { action: "Endre deletekst", tiltakstype: tiltak.tiltakstype.navn },
    });
  };

  return (
    <>
      <Modal open={state.modalOpen} onClose={lukkModal} aria-label="modal">
        <Modal.Header closeButton>
          <Heading size="xsmall">Del med bruker</Heading>
          <Heading size="large" level="1">
            {overskrift(tiltak)}
          </Heading>
        </Modal.Header>

        <Modal.Body className="pb-0">
          <DelMedBrukerContent
            state={state}
            dispatch={dispatch}
            veiledernavn={veiledernavn}
            brukernavn={bruker.fornavn}
            harDeltMedBruker={harDeltMedBruker}
            tiltak={tiltak}
            enableRedigerDeletekst={enableRedigerDeletekst}
          />
        </Modal.Body>

        <Modal.Footer className="grid pt-5">
          <div className="flex items-center gap-5">
            {enableRedigerDeletekst ? null : (
              <Button size="small" onClick={enableEndreDeletekst} variant="secondary">
                Rediger melding
              </Button>
            )}
            <div className="flex">
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
                        tiltakstype: tiltak.tiltakstype.navn,
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
            Kandidatene vil få et varsel fra Nav, og kan logge inn på nav.no for å lese meldingen.
          </BodyShort>
          <Separator />
          <HStack gap="4">
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
          </HStack>
        </Modal.Footer>
      </Modal>

      {sendtStatus === "SENDING_FEILET" && (
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

      {sendtStatus === "SENDT_OK" && dialogId !== null && (
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
              dialogId,
            });
          }}
          secondaryButtonText="Lukk"
          secondaryButtonOnClick={lukkStatusmodal}
        />
      )}
    </>
  );
}
