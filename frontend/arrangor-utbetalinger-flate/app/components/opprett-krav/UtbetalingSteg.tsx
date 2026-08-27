import { Heading, TextField, VStack } from "@navikt/ds-react";
import { useEffect } from "react";
import { errorAt } from "~/utils/validering";
import { BetalingsinformasjonInput } from "../utbetaling/BetalingsinformasjonInput";
import { OpprettKravFormState } from "~/routes/$orgnr.opprett-krav.$gjennomforingid";
import { FieldError, OpprettKravUtbetalingSteg } from "@arrangor-utbetalinger/api-client";

interface UtbetalingStepProps {
  data: OpprettKravUtbetalingSteg;
  formState: OpprettKravFormState;
  updateFormState: (updates: Partial<OpprettKravFormState>) => void;
  errors: FieldError[];
  onRevalidate: () => void;
}

export default function UtbetalingSteg({
  data,
  formState,
  updateFormState,
  errors,
  onRevalidate,
}: UtbetalingStepProps) {
  useEffect(() => {
    if (data.kontonummer && !formState.kontonummer) {
      updateFormState({ kontonummer: data.kontonummer });
    }
  }, [data.kontonummer, formState.kontonummer, updateFormState]);

  return (
    <VStack gap="space-16">
      <Heading size="large" level="3">
        Utbetalingsinformasjon
      </Heading>
      <TextField
        label="Beløp til utbetaling"
        description="Oppgi samlet beløp som skal faktureres Nav for denne utbetalingsperioden"
        value={formState.belop || ""}
        onChange={(e) => updateFormState({ belop: e.target.value })}
        error={errorAt("/belop", errors)}
        type="number"
        htmlSize={15}
        size="small"
        name="belop"
        id="belop"
      />
      <BetalingsinformasjonInput
        kontonummer={data.kontonummer}
        kontonummerError={errorAt("/kontonummer", errors)}
        onSyncKontonummer={onRevalidate}
        kid={formState.kid || ""}
        onKidChange={(kid) => updateFormState({ kid })}
      />
    </VStack>
  );
}
