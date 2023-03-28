import { Heading, Modal } from "@navikt/ds-react";
import React, { useState } from "react";
import styles from "./Modal.module.scss";
import { OpprettAvtaleContainer } from "./OpprettAvtaleContainer";
import { StatusModal } from "mulighetsrommet-veileder-flate/src/components/modal/delemodal/StatusModal";
import { porten } from "mulighetsrommet-veileder-flate/src/constants";
import {useNavigerTilAvtale} from "../../../hooks/useNavigerTilAvtale";

interface OpprettAvtaleModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  shouldCloseOnOverlayClick?: boolean;
}

const OpprettAvtaleModal = ({
  modalOpen,
  onClose,
  handleForm,
  handleCancel,
}: OpprettAvtaleModalProps) => {
  const { navigerTilAvtale } = useNavigerTilAvtale();
  const clickSend = () => {
    handleForm?.();
  };

  const clickCancel = () => {
    onClose();
    handleCancel!();
  };

  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<string | null>(null);

  return (
    <>
      {!error && !result && (
        <Modal
          shouldCloseOnOverlayClick={false}
          closeButton
          open={modalOpen}
          onClose={onClose}
          className={styles.overstyrte_styles_fra_ds_modal}
          aria-label="modal"
        >
          <Modal.Content>
            <Heading size="small" level="2" data-testid="modal_header">
              Registrer ny avtale
            </Heading>
            <OpprettAvtaleContainer setError={setError} setResult={setResult} />
          </Modal.Content>
        </Modal>
      )}
      {error && (
        <StatusModal
          modalOpen={modalOpen}
          ikonVariant="error"
          heading="Kunne ikke opprette avtale"
          text={
            <>
              Avtalen kunne ikke opprettes på grunn av en teknisk feil hos oss.
              Forsøk på nytt eller ta <a href={porten}>kontakt</a> i Porten
              dersom du trenger mer hjelp.
            </>
          }
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
          heading="Avtalen er opprettet."
          text="Avtalen ble opprettet."
          primaryButtonText="Gå til avtalen"
          primaryButtonOnClick={() => navigerTilAvtale(result)}
          secondaryButtonText="Lukk"
          secondaryButtonOnClick={() => clickCancel()}
        />
      )}
    </>
  );
};

export default OpprettAvtaleModal;
