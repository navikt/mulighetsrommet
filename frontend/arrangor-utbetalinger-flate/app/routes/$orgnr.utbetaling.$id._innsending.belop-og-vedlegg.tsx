import {
  BodyShort,
  Button,
  ErrorSummary,
  FileObject,
  Heading,
  HStack,
  TextField,
  VStack,
} from "@navikt/ds-react";
import type { FieldError } from "@arrangor-utbetalinger/api-client";
import { SubmitEvent, useRef, useState } from "react";
import { MetaFunction, useLocation } from "react-router";
import { useIdFromUrl } from "~/utils/navigation";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useUtbetalingWizard } from "~/hooks/useUtbetalingWizard";
import { VedleggUpload } from "~/components/utbetaling/VedleggUpload";
import { errorAt } from "~/utils/validering";

export const meta: MetaFunction = () => {
  return [
    { title: "Beløp og vedlegg - Godkjenn innsending" },
    {
      name: "description",
      content: "Registrer beløp og last opp vedlegg for utbetalingen",
    },
  ];
};

export default function BelopOgVedlegg() {
  const id = useIdFromUrl();
  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const wizard = useUtbetalingWizard(utbetaling);
  const { belop: previousBelop, vedlegg: previousVedlegg } = useLocation().state || {};

  const [belop, setBelop] = useState(previousBelop != null ? String(previousBelop) : "");
  const [vedlegg, setVedlegg] = useState<FileObject[]>(
    (previousVedlegg ?? []).map((file: File): FileObject => ({ file, error: false })),
  );
  const [errors, setErrors] = useState<FieldError[]>([]);
  const errorSummaryRef = useRef<HTMLDivElement>(null);

  const handleSubmit = (e: SubmitEvent) => {
    e.preventDefault();

    const newErrors: FieldError[] = [];
    const belopNumber = Number(belop);

    if (!belop || isNaN(belopNumber) || !Number.isInteger(belopNumber) || belopNumber <= 0) {
      newErrors.push({ pointer: "/belop", detail: "Beløp må være et heltall større enn 0" });
    }
    if (vedlegg.length === 0) {
      newErrors.push({ pointer: "/vedlegg", detail: "Minst ett vedlegg er påkrevd" });
    }

    if (newErrors.length > 0) {
      setErrors(newErrors);
      errorSummaryRef.current?.focus();
      return;
    }

    wizard.goToNext({
      belop: belopNumber,
      vedlegg: vedlegg.map(({ file }) => file),
    });
  };

  return (
    <VStack gap="space-16">
      <Heading level="2" spacing size="large">
        Beløp og vedlegg
      </Heading>
      <BodyShort>
        Oppgi det totale beløpet dere krever utbetalt for perioden, og last opp vedlegg som
        dokumenterer kravet (f.eks. timelogg).
      </BodyShort>

      {errors.length > 0 && (
        <ErrorSummary ref={errorSummaryRef} heading="Feil i skjemaet">
          {errors.map((error) => (
            <ErrorSummary.Item key={error.pointer} href={`#${error.pointer.replace("/", "")}`}>
              {error.detail}
            </ErrorSummary.Item>
          ))}
        </ErrorSummary>
      )}

      <form onSubmit={handleSubmit}>
        <VStack gap="space-16">
          <TextField
            id="belop"
            label="Beløp (NOK)"
            description="Oppgi totalt beløp i hele kroner"
            value={belop}
            onChange={(e) => setBelop(e.target.value)}
            error={errorAt("/belop", errors)}
            inputMode="numeric"
          />

          <VedleggUpload
            files={vedlegg}
            onFilesChange={setVedlegg}
            description="Last opp dokumenter som dokumenterer kravet (f.eks. timelogg)"
            error={errorAt("/vedlegg", errors)}
          />

          <HStack gap="space-16">
            <Button type="button" variant="tertiary" onClick={wizard.goToPrevious}>
              Tilbake
            </Button>
            <Button type="submit">Neste</Button>
          </HStack>
        </VStack>
      </form>
    </VStack>
  );
}
