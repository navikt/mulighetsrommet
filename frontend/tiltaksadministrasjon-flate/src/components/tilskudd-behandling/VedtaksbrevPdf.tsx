import { Loader } from "@navikt/ds-react";
import { useEffect, useState } from "react";

// Gjemmer pdf-toolbaren i Edge/Chrome
const HIDE_TOOLBAR_HASH = "#toolbar=0";

interface Props {
  blob: Blob | undefined;
  isLoading: boolean;
  isError: boolean;
}

export function VedtaksbrevPdf({ blob, isLoading, isError }: Props) {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!blob) return;
    const url = URL.createObjectURL(blob);
    setBlobUrl(url + HIDE_TOOLBAR_HASH);
    return () => {
      URL.revokeObjectURL(url);
      setBlobUrl(null);
    };
  }, [blob]);

  return (
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
  );
}
