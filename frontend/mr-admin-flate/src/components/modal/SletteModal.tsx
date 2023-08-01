import { Button, Heading, Modal } from "@navikt/ds-react";
import { ApiError } from "mulighetsrommet-api-client";
import styles from "../modal/Modal.module.scss";
import {
  ExclamationmarkTriangleFillIcon,
  XMarkOctagonFillIcon,
} from "@navikt/aksel-icons";
import classNames from "classnames";
import { UseMutationResult } from "@tanstack/react-query";

interface Props {
  modalOpen: boolean;
  onClose: () => void;
  mutation: UseMutationResult<string, unknown, string>;
  handleDelete: () => void;
  headerText: string;
  headerTextError: string;
  dataTestId?: string;
}

const SletteModal = ({
  modalOpen,
  onClose,
  mutation,
  handleDelete,
  headerText,
  headerTextError,
  dataTestId,
}: Props) => {
  const clickCancel = () => {
    onClose();
  };

  function headerInnhold() {
    return (
      <div className={styles.heading}>
        {mutation.isError ? (
          <>
            <ExclamationmarkTriangleFillIcon className={styles.erroricon} />
            <Heading size={"medium"}>{headerTextError}</Heading>
          </>
        ) : (
          <>
            <XMarkOctagonFillIcon className={styles.warningicon} />
            <Heading size="medium">{headerText}</Heading>
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
          <p>Du kan ikke angre denne handlingen.</p>
        )}

        <div className={styles.knapperad}>
          <Button variant="secondary" onClick={clickCancel}>
            Avbryt
          </Button>
          {mutation?.isError ? null : (
            <Button
              variant="danger"
              onClick={handleDelete}
              data-testid={dataTestId}
            >
              Slett
            </Button>
          )}
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

export default SletteModal;
