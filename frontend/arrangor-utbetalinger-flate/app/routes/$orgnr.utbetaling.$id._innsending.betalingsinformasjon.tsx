import { ErrorSummary, Heading, TextField, VStack } from "@navikt/ds-react";
import { useRef, useState } from "react";
import { MetaFunction, useLocation } from "react-router";
import { FieldError } from "@arrangor-utbetalinger/api-client";
import { BetalingsinformasjonInput } from "~/components/utbetaling/BetalingsinformasjonInput";
import { StepFooter } from "~/components/utbetaling/StepFooter";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useSyncKontonummer } from "~/hooks/useSyncKontonummer";
import { useUtbetalingWizard } from "~/hooks/useUtbetalingWizard";
import { useIdFromUrl } from "~/utils/navigation";
import { errorAt } from "~/utils/validering";

export const meta: MetaFunction = () => {
  return [
    { title: "Betalingsinformasjon - Godkjenn innsending" },
    {
      name: "description",
      content: "Kontonummer og KID-nummer for utbetalingen",
    },
  ];
};

export default function Betalingsinformasjon() {
  const id = useIdFromUrl();
  const { kid: kidFromState, belop: belopFromState } = useLocation().state || {};

  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const syncKontonummer = useSyncKontonummer(id);
  const wizard = useUtbetalingWizard(utbetaling);
  const prisKreverRegistrering = utbetaling.beregning.pris.type === "KREVER_REGISTRERING";

  const [kid, setKid] = useState(kidFromState ?? utbetaling.betalingsinformasjon?.kid ?? "");
  const [belop, setBelop] = useState(belopFromState != null ? String(belopFromState) : "");
  const [errors, setErrors] = useState<FieldError[]>([]);
  const errorSummaryRef = useRef<HTMLDivElement>(null);

  const validateAndContinue = () => {
    const errors: FieldError[] = [];

    if (!utbetaling.betalingsinformasjon?.kontonummer) {
      errors.push({ pointer: "/kontonummer", detail: "Fant ikke kontonummer" });
    }

    const belopAsNumber = Number(belop);
    if (
      prisKreverRegistrering &&
      (!belop || isNaN(belopAsNumber) || !Number.isInteger(belopAsNumber) || belopAsNumber <= 0)
    ) {
      errors.push({ pointer: "/belop", detail: "Beløp må være et heltall større enn 0" });
    }

    if (errors.length > 0) {
      setErrors(errors);
      errorSummaryRef.current?.focus();
      return;
    }

    const values = prisKreverRegistrering ? { kid, belop: belopAsNumber } : { kid };
    wizard.goToNext(values);
  };

  return (
    <VStack gap="space-16">
      <Heading level="2" spacing size="large">
        Betalingsinformasjon
      </Heading>

      {errors.length > 0 && (
        <ErrorSummary ref={errorSummaryRef} heading="Feil i skjemaet">
          {errors.map((error) => (
            <ErrorSummary.Item key={error.pointer} href={`#${error.pointer.replace("/", "")}`}>
              {error.detail}
            </ErrorSummary.Item>
          ))}
        </ErrorSummary>
      )}

      {prisKreverRegistrering && (
        <TextField
          label="Beløp til utbetaling"
          description="Oppgi samlet beløp som skal faktureres Nav for denne utbetalingsperioden"
          value={belop}
          onChange={(e) => setBelop(e.target.value)}
          error={errorAt("/belop", errors)}
          inputMode="numeric"
          htmlSize={15}
          size="small"
          name="belop"
          id="belop"
        />
      )}

      <BetalingsinformasjonInput
        kontonummer={utbetaling.betalingsinformasjon?.kontonummer}
        kontonummerError={errorAt("/kontonummer", errors)}
        onSyncKontonummer={() => syncKontonummer.mutate()}
        kid={kid}
        onKidChange={setKid}
      />
      <StepFooter wizard={wizard} primaryAction={{ onClick: validateAndContinue }} />
    </VStack>
  );
}
