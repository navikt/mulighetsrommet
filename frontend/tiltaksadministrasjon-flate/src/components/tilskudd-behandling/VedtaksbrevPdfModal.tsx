import { Button, HStack, Loader, Modal } from "@navikt/ds-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { Document, Page, pdfjs } from "react-pdf";
import "react-pdf/dist/Page/AnnotationLayer.css";
import "react-pdf/dist/Page/TextLayer.css";

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.mjs",
  import.meta.url,
).toString();

interface Props {
  blob: Blob | undefined;
  isLoading: boolean;
  isError: boolean;
  open: boolean;
  onClose: () => void;
}

export function VedtaksbrevPdfModal({ blob, isLoading, isError, open, onClose }: Props) {
  const [pdfBytes, setPdfBytes] = useState<Uint8Array | undefined>();
  const [numPages, setNumPages] = useState<number>(0);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!blob || !open) {
      setPdfBytes(undefined);
      setNumPages(0);
      return;
    }
    let cancelled = false;
    blob
      .arrayBuffer()
      .then((buffer) => {
        if (!cancelled) setPdfBytes(new Uint8Array(buffer));
        return undefined;
      })
      .catch(() => {
        // Ignore errors reading PDF blob
      });
    return () => {
      cancelled = true;
    };
  }, [blob, open]);

  // Stable object reference — prevents react-pdf from seeing a "changed" file prop on re-render.
  const pdfFile = useMemo(() => (pdfBytes ? { data: pdfBytes } : null), [pdfBytes]);

  const pageWidth = (containerRef.current?.clientWidth ?? 740) - 48;

  return (
    <Modal
      open={open}
      onClose={onClose}
      header={{ heading: "Forhåndsvisning av vedtaksbrev" }}
      width="800px"
    >
      <Modal.Body>
        <div ref={containerRef} className="flex flex-col items-center min-h-96">
          {isLoading && (
            <div className="flex justify-center items-center h-96">
              <Loader size="xlarge" title="Laster PDF..." />
            </div>
          )}
          {isError && (
            <p className="text-center text-red-600">
              Kunne ikke laste vedtaksbrevet. Prøv å laste ned filen i stedet.
            </p>
          )}
          {pdfFile && (
            <Document
              file={pdfFile}
              onLoadSuccess={({ numPages }) => setNumPages(numPages)}
              loading={
                <div className="flex justify-center items-center h-96">
                  <Loader size="xlarge" title="Laster PDF..." />
                </div>
              }
              error={<p className="text-center">Feil ved innlasting av PDF.</p>}
            >
              {Array.from({ length: numPages }, (_, i) => (
                <Page key={i + 1} pageNumber={i + 1} width={pageWidth} className="shadow-md mb-4" />
              ))}
            </Document>
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
