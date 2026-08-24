import { Button, HStack, Loader, Modal } from "@navikt/ds-react";
import { useEffect, useState } from "react";

interface Props {
  blob: Blob | undefined;
  isLoading: boolean;
  isError: boolean;
  open: boolean;
  onClose: () => void;
}

export function VedtaksbrevPdfModal({ blob, isLoading, isError, open, onClose }: Props) {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!blob || !open) return;
    const url = URL.createObjectURL(blob);
    setBlobUrl(url);
    return () => {
      URL.revokeObjectURL(url);
      setBlobUrl(null);
    };
  }, [blob, open]);

  return (
    <Modal
      open={open}
      onClose={onClose}
      header={{ heading: "Forhåndsvisning av vedtaksbrev" }}
      width="1200px"
    >
      <Modal.Body>
        <div className="flex flex-col items-center min-h-96">
          {isLoading && (
            <div className="flex justify-center items-center h-96">
              <Loader size="xlarge" title="Laster PDF..." />
            </div>
          )}
          {isError && <p className="text-center text-red-600">Kunne ikke laste vedtaksbrevet.</p>}
          {blobUrl && (
            <iframe
              src={blobUrl}
              title="Forhåndsvisning av vedtaksbrev"
              className="w-full"
              style={{ height: "70vh", border: "none" }}
            />
          )}
        </div>
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
