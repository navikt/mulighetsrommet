import { Button, Heading, Modal } from "@navikt/ds-react";
import { ApiError, Avtale } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import styles from "../modal/Modal.module.scss";
import {
  ExclamationmarkTriangleFillIcon,
  XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";
import classNames from "classnames";
import { useDeleteAvtalenotat } from "../../api/avtaler/avtalenotat/useDeleteAvtalenotat";

interface SletteNotatModalProps {
  modalOpen: boolean;
  onClose: () => void;
  handleForm?: () => void;
  handleCancel?: () => void;
  avtale?: Avtale;
}

const SletteNotatModal = ({
  modalOpen,
  onClose,
  handleCancel,
  avtale,
}: SletteNotatModalProps) => {
  const mutation = useDeleteAvtalenotat();
  const navigate = useNavigate();
  useEffect(() => {
    Modal.setAppElement("#root");
  }, []);

  useEffect(() => {
    if (mutation.isSuccess) {
      navigate("/notater/avtaler");
      return;
    }
  }, [mutation]);

  const clickCancel = () => {
    onClose();
    handleCancel?.();
  };

  const handleDelete = () => {
    if (avtale?.id) {
      mutation.mutate(avtale?.id);
    }
  };

  function headerInnhold() {
    return (
      <div className={styles.heading}>
        {mutation.isError ? (
          <>
            <ExclamationmarkTriangleFillIcon className={styles.erroricon} />
            <span>Kan ikke slette notatet.</span>
          </>
        ) : (
          <>
            <XMarkOctagonFillIcon className={styles.warningicon} />
            <span>Ønsker du å slette notatet?</span>
          </>
        )}
      </div>
    );
  }

  function modalInnhold() {
    return (
      <>
        {mutation?.isError ? (
          <p>{(mutation.error as ApiError).body}</p>
        ) : (
          <p>Du kan ikke angre denne handlingen</p>
        )}
        <div className={styles.knapperad}>
          {mutation?.isError ? null : (
            <Button variant="danger" onClick={handleDelete}>
              Slett notat
            </Button>
          )}

          <Button variant="secondary-neutral" onClick={clickCancel}>
            Avbryt
          </Button>
        </div>
      </>
    );
  }

  return (
    <>
      <Modal
        shouldCloseOnOverlayClick={false}
        closeButton
        open={modalOpen}
        onClose={clickCancel}
        className={classNames(
          styles.overstyrte_styles_fra_ds_modal,
          styles.text_center,
        )}
        aria-label="modal"
      >
        <Modal.Content>
          <Heading
            size="medium"
            level="2"
            data-testid="slett_avtale_modal_header"
          >
            {headerInnhold()}
          </Heading>
          {modalInnhold()}
        </Modal.Content>
      </Modal>
    </>
  );
};

export default SletteNotatModal;
