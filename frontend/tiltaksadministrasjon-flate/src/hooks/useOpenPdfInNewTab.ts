import { useState } from "react";

const PDF_LOAD_ERROR = "Kunne ikke laste PDF-en. Prøv igjen.";
const POPUP_BLOCKED_ERROR =
  "Nettleseren blokkerte åpning av PDF-en. Tillat popup-vinduer og prøv igjen.";

export function useOpenPdfInNewTab(): {
  open: (fetchBlob: () => Promise<Blob>) => Promise<void>;
  feilmelding: string | null;
} {
  const [feilmelding, setFeilmelding] = useState<string | null>(null);

  async function open(fetchBlob: () => Promise<Blob>) {
    const vindu = window.open("", "_blank");
    if (!vindu) {
      setFeilmelding(POPUP_BLOCKED_ERROR);
      return;
    }

    vindu.opener = null;
    vindu.document.title = "Laster PDF...";
    vindu.document.body.textContent = "Laster PDF...";
    setFeilmelding(null);

    let url: string | undefined;
    try {
      const blob = await fetchBlob();
      const objectUrl = URL.createObjectURL(blob);
      url = objectUrl;
      vindu.location.replace(objectUrl);
      setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
    } catch {
      if (url) {
        URL.revokeObjectURL(url);
      }
      vindu.close();
      setFeilmelding(PDF_LOAD_ERROR);
    }
  }

  return { open, feilmelding };
}
