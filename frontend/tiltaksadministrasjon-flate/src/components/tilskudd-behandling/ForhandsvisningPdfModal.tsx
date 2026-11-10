import { Button, HStack, Modal } from "@navikt/ds-react";
import { ForhandsvisningPdf } from "./ForhandsvisningPdf";

interface Props {
  blob: Blob | undefined;
  isLoading: boolean;
  isError: boolean;
  open: boolean;
  onClose: () => void;
  title: string;
}

export function ForhandsvisningPdfModal({ blob, isLoading, isError, open, onClose, title }: Props) {
  return (
    <Modal open={open} onClose={onClose} header={{ heading: title }} width="1200px">
      <Modal.Body>
        {open && (
          <ForhandsvisningPdf title={title} blob={blob} isLoading={isLoading} isError={isError} />
        )}
      </Modal.Body>
      <Modal.Footer>
        <HStack justify="end">
          <Button variant="secondary" size="small" onClick={onClose}>
            Lukk
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
