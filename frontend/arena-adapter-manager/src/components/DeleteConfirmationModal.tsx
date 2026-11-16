import { BodyShort, Button, Modal } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  title: string;
  description: string;
  onConfirm: () => Promise<boolean>;
  onClose: () => void;
}

export function DeleteConfirmationModal({ title, description, onConfirm, onClose }: Props) {
  const [isDeleting, setIsDeleting] = useState(false);

  async function handleDelete() {
    setIsDeleting(true);
    try {
      if (await onConfirm()) {
        onClose();
      }
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <Modal
      open
      header={{ heading: title }}
      onClose={() => {
        if (!isDeleting) onClose();
      }}
      width={500}
    >
      <Modal.Body>
        <BodyShort>{description} This cannot be undone.</BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <Button data-color="danger" loading={isDeleting} onClick={handleDelete}>
          Delete
        </Button>
        <Button variant="secondary" disabled={isDeleting} onClick={onClose}>
          Cancel
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
