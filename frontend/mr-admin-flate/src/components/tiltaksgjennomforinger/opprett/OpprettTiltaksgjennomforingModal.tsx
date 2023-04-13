import { Heading, Modal } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import styles from "./Modal.module.scss";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { porten } from "mulighetsrommet-veileder-flate/src/constants";
import { useNavigerTilTiltaksgjennomforing } from "../../../hooks/useNavigerTilTiltaksgjennomforing";
import { OpprettTiltaksgjennomforingContainer } from "./OpprettTiltaksgjennomforingContainer";

interface ModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  shouldCloseOnOverlayClick?: boolean;
}

export const OpprettTiltaksgjennomforingModal = ({
  modalOpen,
  onClose,
  handleCancel,
}: ModalProps) => {
  const { navigerTilTiltaksgjennomforing } =
    useNavigerTilTiltaksgjennomforing();
  useEffect(() => {
    Modal.setAppElement("#root");
  });

  const clickCancel = () => {
    setError(false);
    setResult(null);
    onClose();
    handleCancel?.();
  };

  const [error, setError] = useState<boolean>(false);
  const [result, setResult] = useState<string | null>(null);

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
              Opprett ny gjennomføring
            </Heading>
            <OpprettTiltaksgjennomforingContainer
              onAvbryt={clickCancel}
              setError={setError}
              setResult={setResult}
            />
          </Modal.Content>
        </Modal>
      )}
      {error && (
        <StatusModal
          modalOpen={modalOpen}
          ikonVariant="error"
          heading="Kunne ikke opprette gjennomføring"
          text={
            <>
              Gjennomføringen kunne ikke opprettes på grunn av en teknisk feil
              hos oss. Forsøk på nytt eller ta <a href={porten}>kontakt</a> i
              Porten dersom du trenger mer hjelp.
            </>
          }
          onClose={clickCancel}
          primaryButtonOnClick={() => setError(false)}
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
