import {
  BodyShort,
  ErrorSummary,
  FileObject,
  GuidePanel,
  Heading,
  Link,
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
import { StepFooter } from "~/components/utbetaling/StepFooter";

export const meta: MetaFunction = () => {
  return [
    { title: "Vedlegg - Godkjenn innsending" },
    {
      name: "description",
      content: "Last opp vedlegg for utbetalingen",
    },
  ];
};

export default function Vedlegg() {
  const id = useIdFromUrl();
  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const wizard = useUtbetalingWizard(utbetaling);
  const { vedlegg: previousVedlegg } = useLocation().state || {};

  const [vedlegg, setVedlegg] = useState<FileObject[]>(
    (previousVedlegg ?? []).map((file: File): FileObject => ({ file, error: false })),
  );
  const [errors, setErrors] = useState<FieldError[]>([]);
  const errorSummaryRef = useRef<HTMLDivElement>(null);

  const submit = () => {
    const newErrors: FieldError[] = [];

    if (vedlegg.length === 0) {
      newErrors.push({ pointer: "/vedlegg", detail: "Minst ett vedlegg er påkrevd" });
    }

    if (newErrors.length > 0) {
      setErrors(newErrors);
      errorSummaryRef.current?.focus();
      return;
    }

    wizard.goToNext({ vedlegg: vedlegg.map(({ file }) => file) });
  };

  const handleSubmit = (e: SubmitEvent) => {
    e.preventDefault();
    submit();
  };

  return (
    <VStack gap="space-16">
      <Heading level="2" spacing size="large">
        Vedlegg
      </Heading>

      <GuidePanel>
        <BodyShort spacing>
          Fakturering skal skje i henhold til prisbilag i avtalen og eventuelle presiseringer. Dere
          må sikre at opplysningene dere oppgir er korrekte.
        </BodyShort>
        <BodyShort spacing>Det skal kun faktureres for faktisk medgått tid.</BodyShort>
        <BodyShort spacing>
          Nav vil kunne gjennomføre kontroller og kreve innsyn for å verifisere at tjenesten og
          tilhørerende fakturering er i henhold til avtalen.
        </BodyShort>
        <BodyShort>
          <Link
            inlineText
            target="_blank"
            href="https://www.nav.no/samarbeidspartner/faktura-tiltak/#fakturavedlegg"
          >
            Fakturavedleggsskjema
          </Link>{" "}
          eller tilsvarende dokumentasjon skal lastes opp under.
        </BodyShort>
      </GuidePanel>

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
        <VedleggUpload
          files={vedlegg}
          onFilesChange={setVedlegg}

          error={errorAt("/vedlegg", errors)}
        />

        <StepFooter wizard={wizard} primaryAction={{ onClick: submit }} />
      </form>
    </VStack>
  );
}
