import { FormButtons } from "@/components/skjema/FormButtons";

export function AvtaleFormKnapperad({ heading }: { heading?: string }) {
  return <FormButtons heading={heading} submitLabel="Lagre redigert avtale" />;
}
