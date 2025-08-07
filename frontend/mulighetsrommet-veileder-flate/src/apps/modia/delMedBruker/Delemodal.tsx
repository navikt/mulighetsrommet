import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";
import { useDelTiltakMedBruker } from "@/api/queries/useDelTiltakMedBruker";
import { ModiaRoute, navigateToModiaApp } from "@/apps/modia/ModiaRoute";
import { PortenLink } from "@/components/PortenLink";
import { StatusModal } from "@/components/modal/StatusModal";
import { Separator } from "@/utils/Separator";
import { erPreview } from "@/utils/Utils";
import { Brukerdata, DeltMedBrukerDto, VeilederflateTiltak } from "@api-client";
import { BodyShort, Button, Checkbox, Heading, HelpText, HStack, Modal } from "@navikt/ds-react";
import { DelMedBrukerContent, MAKS_ANTALL_TEGN_DEL_MED_BRUKER } from "./DelMedBrukerContent";
import { Actions, State } from "./DelemodalActions";

interface DelemodalProps {
  veiledernavn?: string;
  tiltak: VeilederflateTiltak;
  bruker: Brukerdata;
  deltMedBruker?: DeltMedBrukerDto;
  dispatch: (action: Actions) => void;
  state: State;
  veilederEnhet: string;
}

function overskrift(tiltak: VeilederflateTiltak): string {
  return `Tiltak gjennom Nav: ${tiltak.tiltakstype.navn}`;
}

export function Delemodal({
  veiledernavn,
  tiltak,
  bruker,
  deltMedBruker,
  dispatch,
  state,
  veilederEnhet,
}: DelemodalProps) {
  const mutation = useDelTiltakMedBruker({
    onSuccess: (response) => {
      dispatch({ type: "Sendt ok", payload: response.dialogId });
      mutation.reset();
    },
    onError: () => {
      dispatch({ type: "Sending feilet" });
    },
  });

  const senderTilDialogen = state.sendtStatus === "SENDER";
  const { enableRedigerDeletekst, sendtStatus, dialogId } = state;

  const originaltekstLengde = state.originalDeletekst.length;
  const lukkStatusmodal = () => dispatch({ type: "Toggle statusmodal", payload: false });
  const lukkModal = () => dispatch({ type: "Toggle modal", payload: false });

  const clickCancel = () => {
    lukkModal();
    dispatch({ type: "Avbryt" });
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
      tiltakstypeId: tiltak.tiltakstype.id,
      deltFraEnhet: veilederEnhet,
    });
  };

  const enableEndreDeletekst = () => {
    dispatch({ type: "Enable rediger deletekst", payload: true });
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
            deltMedBruker={deltMedBruker}
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
