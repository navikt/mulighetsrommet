import { Heading, Modal } from "@navikt/ds-react";
import React, { useEffect, useState } from "react";
import styles from "./Modal.module.scss";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { useNavigerTilTiltaksgjennomforing } from "../../hooks/useNavigerTilTiltaksgjennomforing";
import { OpprettTiltaksgjennomforingContainer } from "./OpprettTiltaksgjennomforingContainer";
import { Avtale, Tiltaksgjennomforing } from "mulighetsrommet-api-client";

interface ModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  shouldCloseOnOverlayClick?: boolean;
  tiltaksgjennomforing?: Tiltaksgjennomforing;
  avtale: Avtale;
}

export const OpprettTiltaksgjennomforingModal = ({
  modalOpen,
  onClose,
  handleCancel,
  tiltaksgjennomforing,
  avtale,
}: ModalProps) => {
  const { navigerTilTiltaksgjennomforing } =
    useNavigerTilTiltaksgjennomforing();
  useEffect(() => {
    Modal.setAppElement("#root");
  });

  const clickCancel = () => {
    setError(null);
    setResult(null);
    onClose();
    handleCancel?.();
  };

  const [error, setError] = useState<React.ReactNode | null>(null);
  const [result, setResult] = useState<string | null>(null);

  const redigeringsModus = !!tiltaksgjennomforing;

  return (
    <>
      {!error && !result && (
        <Modal
          shouldCloseOnOverlayClick={false}
          closeButton
          open={modalOpen}
          onClose={clickCancel}
          className={styles.overstyrte_styles_fra_ds_modal}
          aria-label="modal"
        >
          <Modal.Content>
            <Heading size="medium" level="2" data-testid="modal_header">
              { redigeringsModus ? "Rediger gjennomføring" : "Opprett ny gjennomføring" }
            </Heading>
            <OpprettTiltaksgjennomforingContainer
              onAvbryt={clickCancel}
              setError={setError}
              setResult={setResult}
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
          onClose={clickCancel}
          primaryButtonOnClick={() => setError(null)}
          primaryButtonText="Prøv igjen"
          secondaryButtonOnClick={clickCancel}
          secondaryButtonText="Avbryt"
        />
      )}
      {result && (
        <StatusModal
          modalOpen={modalOpen}
          onClose={clickCancel}
          ikonVariant="success"
          heading="Gjennomføringen er opprettet."
          text="Gjennomføringen ble opprettet."
          primaryButtonText="Gå til gjennomføringen"
          primaryButtonOnClick={() => navigerTilTiltaksgjennomforing(result)}
          secondaryButtonText="Lukk"
          secondaryButtonOnClick={clickCancel}
        />
      )}
    </>
  );
};
