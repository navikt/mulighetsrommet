import { ApiError } from "mulighetsrommet-api-client";
import { useDeleteVirksomhetKontaktperson } from "../../api/virksomhet/useDeleteVirksomhetKontaktperson";
import { Button, Modal } from "@navikt/ds-react";
import { useEffect } from "react";
import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import styles from "../modal/Modal.module.scss";
import classNames from "classnames";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  kontaktpersonId?: string;
}

export const DeleteVirksomhetKontaktpersonModal = ({
  modalOpen,
  onClose,
  kontaktpersonId,
}: Props) => {
  const mutation = useDeleteVirksomhetKontaktperson();

  useEffect(() => {
    if (mutation.isSuccess) {
      mutation.reset();
      onClose();
      return;
    }
  }, [mutation]);

  const handleDelete = () => {
    if (kontaktpersonId) {
      mutation.mutate(kontaktpersonId);
    }
  };

  const close = () => {
    mutation.reset();
    onClose();
  };

  return (
    <>
      <Modal
        open={modalOpen}
        className={classNames(
          styles.overstyrte_styles_fra_ds_modal,
          styles.text_center,
        )}
        onClose={close}
        aria-label="modal"
      >
        <Modal.Header closeButton>
          <div className={styles.heading}>
            <XMarkOctagonFillIcon className={styles.warningicon} />
            {mutation.isError ? (
              <span>Kan ikke slette</span>
            ) : (
              <span>Ønsker du å slette?</span>
            )}
          </div>
        </Modal.Header>
        <Modal.Body>
          {mutation?.isError ? (
            <p>{(mutation.error as ApiError).body}</p>
          ) : (
            <p>Du kan ikke angre denne handlingen</p>
          )}
          <div className={styles.knapperad}>
            {!mutation?.isError && (
              <Button variant="danger" onClick={handleDelete}>
                Slett kontaktperson
              </Button>
            )}
            <Button variant="secondary-neutral" onClick={close}>
              Avbryt
            </Button>
          </div>
        </Modal.Body>
      </Modal>
    </>
  );
};
