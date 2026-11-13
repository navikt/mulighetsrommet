import { InlineMessage, Loader, VStack } from "@navikt/ds-react";
import { useEffect, useState } from "react";

// Gjemmer pdf-toolbaren i Edge/Chrome
const HIDE_TOOLBAR_HASH = "#toolbar=0";

interface Props {
  blob: Blob | undefined;
  isLoading: boolean;
  isError: boolean;
  title: string;
}

export function ForhandsvisningPdf({ blob, isLoading, isError, title }: Props) {
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
    <VStack align="center">
      {isLoading && <Loader size="xlarge" title="Laster PDF..." />}
      {isError && (
        <InlineMessage status="error">Kunne ikke laste PDF-en. Prøv igjen.</InlineMessage>
      )}
      {blobUrl && <iframe src={blobUrl} title={title} className="w-full h-screen" />}
    </VStack>
  );
}
