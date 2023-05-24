import { Heading, Modal, Search } from "@navikt/ds-react";
import React, { useEffect, useState } from "react";
import styles from "./LeggTilGjennomforingModal.module.scss";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { useNavigerTilTiltaksgjennomforing } from "../../hooks/useNavigerTilTiltaksgjennomforing";
import { TiltaksgjennomforingsTabell } from "../tabell/TiltaksgjennomforingsTabell";

interface ModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  shouldCloseOnOverlayClick?: boolean;
  // tiltaksgjennomforing?: Tiltaksgjennomforing;
}

export const LeggTilGjennomforingModal = ({
  modalOpen,
  onClose,
  handleCancel,
}: // tiltaksgjennomforing,
ModalProps) => {
  const clickCancel = () => {
    setError(null);
    setResult(null);
    onClose();
    handleCancel?.();
  };

  const [error, setError] = useState<React.ReactNode | null>(null);
  const [result, setResult] = useState<string | null>(null);

  const { navigerTilTiltaksgjennomforing } =
    useNavigerTilTiltaksgjennomforing();
  useEffect(() => {
    Modal.setAppElement("#root");
  });

  return (
    <>
      {!error && !result && (
        <Modal
          shouldCloseOnOverlayClick={false}
          closeButton
          open={modalOpen}
          onClose={clickCancel}
          className={styles.modal_container}
          aria-label="modal"
        >
          <Modal.Content className={styles.modal_content}>
            <Heading size="medium" level="2">
              Legg til ny gjennomføring til avtalen
            </Heading>
            <Search
              label={"Søk etter gjennomføring"}
              variant="simple"
              hideLabel={false}
            />

            <TiltaksgjennomforingsTabell
              skjulKolonner={{
                tiltakstype: true,
                arrangor: true,
                status: true,
              }}
              leggTilNyGjennomforingModal
            />
          </Modal.Content>
        </Modal>
      )}
      {error && (
        <StatusModal
          modalOpen={modalOpen}
          ikonVariant="error"
          heading="Kunne ikke legge til gjennomføring"
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
          heading="Gjennomføringen er lagt til."
          text="Gjennomføringen ble lagt til."
          primaryButtonText="Gå til gjennomføringen"
          primaryButtonOnClick={() => navigerTilTiltaksgjennomforing(result)}
          secondaryButtonText="Lukk"
          secondaryButtonOnClick={clickCancel}
        />
      )}
    </>
  );
};
