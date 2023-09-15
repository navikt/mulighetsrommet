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
  headerSubText?: string;
  headerTextError: string;
  dataTestId?: string;
  avbryt?: boolean;
}

const SletteModal = ({
  modalOpen,
  onClose,
  mutation,
  handleDelete,
  headerText,
  headerSubText,
  headerTextError,
  dataTestId,
  avbryt = false,
}: Props) => {
  function headerInnhold() {
    return (
      <div className={styles.heading}>
        {mutation.isError ? (
          <>
            <ExclamationmarkTriangleFillIcon
              className={classNames(styles.icon_error, styles.icon)}
            />
            <Heading size="medium">{headerTextError}</Heading>
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
      <BodyShort>
        {mutation?.isError
          ? (mutation.error as ApiError).body
          : headerSubText || "Du kan ikke angre denne handlingen."}
      </BodyShort>
    );
  }

  function footerInnhold() {
    return (
      <div className={styles.knapperad}>
        <Button variant="secondary" onClick={onClose} type="button">
          Lukk
        </Button>
        {mutation?.isError ? null : (
          <Button
            variant="danger"
            onClick={handleDelete}
            data-testid={dataTestId}
            type="button"
          >
            {avbryt ? "Ja, avbryt" : "Slett"}
          </Button>
        )}
      </div>
    );
  }

  return (
    <Modal open={modalOpen} onClose={onClose} aria-label="modal">
      <Modal.Header closeButton={false}>{headerInnhold()}</Modal.Header>
      <Modal.Body>{modalInnhold()}</Modal.Body>
      <Modal.Footer>{footerInnhold()}</Modal.Footer>
    </Modal>
  );
};

export default SletteModal;
