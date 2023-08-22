import { ApiError } from "mulighetsrommet-api-client";
import { useDeleteVirksomhetKontaktperson } from "../../api/virksomhet/useDeleteVirksomhetKontaktperson";
import { BodyShort, Button, Heading, Modal } from "@navikt/ds-react";
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

  function headerInnhold() {
    return (
      <div className={styles.heading}>
        <XMarkOctagonFillIcon
          className={classNames(styles.icon_warning, styles.icon)}
        />
        <Heading size="medium">
          {mutation.isError ? "Kan ikke slette." : "Ønsker du å slette?"}
        </Heading>
      </div>
    );
  }

  function modalInnhold() {
    return (
      <BodyShort>
        {mutation?.isError
          ? (mutation.error as ApiError).body
          : "Du kan ikke angre denne handlingen."}
      </BodyShort>
    );
  }

  function footerInnhold() {
    return (
      <div className={styles.knapperad}>
        <Button variant="secondary" onClick={close}>
          Avbryt
        </Button>
        {!mutation?.isError && (
          <Button variant="danger" onClick={handleDelete}>
            Slett kontaktperson
          </Button>
        )}
      </div>
    );
  }

  return (
    <Modal open={modalOpen} onClose={close} aria-label="modal">
      <Modal.Header closeButton={false}>{headerInnhold()}</Modal.Header>
      <Modal.Body>{modalInnhold()}</Modal.Body>
      <Modal.Footer>{footerInnhold()}</Modal.Footer>
    </Modal>
  );
};
