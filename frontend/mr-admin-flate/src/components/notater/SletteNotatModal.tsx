import { Button, Heading, Modal } from "@navikt/ds-react";
import { ApiError } from "mulighetsrommet-api-client";
import { useEffect } from "react";
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
  notatIdForSletting: string | null;
}

const SletteNotatModal = ({
  modalOpen,
  onClose,
  handleCancel,
  notatIdForSletting,
}: SletteNotatModalProps) => {
  const mutation = useDeleteAvtalenotat();

  useEffect(() => {
    Modal.setAppElement("#root");
  }, []);

  const clickCancel = () => {
    onClose();
    handleCancel?.();
  };

  const handleDelete = () => {
    if (notatIdForSletting) {
      mutation.mutate(notatIdForSletting, { onSuccess: onClose });
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
          <Heading size="medium" level="2">
            {headerInnhold()}
          </Heading>
          {modalInnhold()}
        </Modal.Content>
      </Modal>
    </>
  );
};

export default SletteNotatModal;
