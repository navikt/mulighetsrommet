import { Button, Heading, Modal } from "@navikt/ds-react";
import { ApiError } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import styles from "../modal/Modal.module.scss";
import {
  ExclamationmarkTriangleFillIcon,
  XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";
import classNames from "classnames";
import invariant from "tiny-invariant";
import { UseMutationResult } from "@tanstack/react-query";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  notatIdForSletting: string;
  mutation: UseMutationResult<string, unknown, string>;
}

const SletteNotatModal = ({
  modalOpen,
  onClose,
  notatIdForSletting,
  mutation,
}: Props) => {
  invariant(notatIdForSletting, "Fant ikke id for å slette notat.");

  useEffect(() => {
    Modal.setAppElement("#root");
  }, []);

  const clickCancel = () => {
    onClose();
  };

  const handleDelete = () => {
    mutation.mutate(notatIdForSletting, { onSuccess: onClose });
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
  );
};

export default SletteNotatModal;
