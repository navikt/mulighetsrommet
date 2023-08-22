import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
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
            <ExclamationmarkTriangleFillIcon
              className={classNames(styles.icon_error, styles.icon)}
            />
            <Heading size={"medium"}>{headerTextError}</Heading>
          </>
        ) : (
          <>
            <XMarkOctagonFillIcon
              className={classNames(styles.icon_warning, styles.icon)}
            />
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
          <BodyShort>{(mutation.error as ApiError).body}</BodyShort>
        ) : (
          <BodyShort>Du kan ikke angre denne handlingen.</BodyShort>
        )}
      </>
    );
  }

  function footerInnhold() {
    return (
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
    );
  }

  return (
    <Modal open={modalOpen} onClose={clickCancel} aria-label="modal">
      <Modal.Header closeButton={false}>{headerInnhold()}</Modal.Header>
      <Modal.Body>{modalInnhold()}</Modal.Body>
      <Modal.Footer>{footerInnhold()}</Modal.Footer>
    </Modal>
  );
};

export default SletteModal;
