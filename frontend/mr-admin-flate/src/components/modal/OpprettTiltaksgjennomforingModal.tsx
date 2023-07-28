import { Heading, Modal } from "@navikt/ds-react";
import React, { useState } from "react";
import styles from "./Modal.module.scss";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { TiltaksgjennomforingSkjemaContainer } from "../tiltaksgjennomforinger/TiltaksgjennomforingSkjemaContainer";
import { Avtale, Tiltaksgjennomforing } from "mulighetsrommet-api-client";

interface ModalProps {
  modalOpen: boolean;
  onClose: () => void;
  onSuccess: (id: string) => void;
  tiltaksgjennomforing?: Tiltaksgjennomforing;
  avtale?: Avtale;
}

export const OpprettTiltaksgjennomforingModal = ({
  modalOpen,
  onClose,
  onSuccess,
  tiltaksgjennomforing,
  avtale,
}: ModalProps) => {
  const [error, setError] = useState<React.ReactNode | null>(null);

  const redigeringsModus = !!tiltaksgjennomforing;

  return (
    <>
      {!error && (
        <Modal
          shouldCloseOnOverlayClick={false}
          shouldCloseOnEsc={false}
          closeButton
          open={modalOpen}
          onClose={onClose}
          className={styles.overstyrte_styles_fra_ds_modal}
          aria-label="modal"
        >
          <Modal.Content>
            <Heading size="medium" level="2" data-testid="modal_header">
              {redigeringsModus
                ? "Rediger gjennomføring"
                : "Opprett gjennomføring"}
            </Heading>
            <TiltaksgjennomforingSkjemaContainer
              onClose={onClose}
              onSuccess={onSuccess}
              setError={setError}
              tiltaksgjennomforing={tiltaksgjennomforing}
              avtale={avtale}
            />
          </Modal.Content>
        </Modal>
      )}
      {error && (
        <StatusModal
          modalOpen={modalOpen}
          ikonVariant="error"
          heading="Kunne ikke opprette gjennomføring"
          text={error}
          onClose={onClose}
          primaryButtonOnClick={() => setError(null)}
          primaryButtonText="Prøv igjen"
          secondaryButtonOnClick={onClose}
          secondaryButtonText="Avbryt"
        />
      )}
    </>
  );
};
