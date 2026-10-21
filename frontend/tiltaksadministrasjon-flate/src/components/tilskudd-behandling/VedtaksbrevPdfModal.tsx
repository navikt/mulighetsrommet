import { Button, HStack, Modal } from "@navikt/ds-react";
import { VedtaksbrevPdf } from "./VedtaksbrevPdf";

interface Props {
  blob: Blob | undefined;
  isLoading: boolean;
  isError: boolean;
  open: boolean;
  onClose: () => void;
}

export function VedtaksbrevPdfModal({ blob, isLoading, isError, open, onClose }: Props) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      header={{ heading: "Forhåndsvisning av vedtaksbrev" }}
      width="1200px"
    >
      <Modal.Body>
        {open && <VedtaksbrevPdf blob={blob} isLoading={isLoading} isError={isError} />}
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
