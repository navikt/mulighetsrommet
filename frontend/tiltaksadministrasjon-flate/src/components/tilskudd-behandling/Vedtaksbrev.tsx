import { useVedtaksbrevPdfBlobPost } from "@/api/tilskudd-behandling/useVedtaksbrevPdfBlob";
import { TilskuddBehandlingRequest, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useEffect } from "react";
import { useFormContext } from "react-hook-form";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { VedtaksbrevPdf } from "@/components/tilskudd-behandling/VedtaksbrevPdf";

export function Vedtaksbrev() {
  const vedtaksbrevMutation = useVedtaksbrevPdfBlobPost();
  const form = useFormContext<TilskuddBehandlingRequest>();
  useEffect(() => {
    const data = form.getValues();
    vedtaksbrevMutation.mutate(data, {
      onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
    });
  }, []);
  return (
    <VedtaksbrevPdf
      blob={vedtaksbrevMutation.data}
      isLoading={vedtaksbrevMutation.isPending}
      isError={vedtaksbrevMutation.isError}
    />
  );
}
