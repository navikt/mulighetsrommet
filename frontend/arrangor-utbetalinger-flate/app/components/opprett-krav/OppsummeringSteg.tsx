import { FieldError, LabeledDataElement, PeriodeType } from "@arrangor-utbetalinger/api-client";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Button, Checkbox, CheckboxGroup, Heading, HStack } from "@navikt/ds-react";
import { useState } from "react";
import { OpprettKravFormState } from "~/routes/$orgnr.opprett-krav.$gjennomforingid";
import { tekster } from "~/tekster";
import { VedleggSummary } from "~/components/utbetaling/VedleggSummary";
import { errorAt } from "~/utils/validering";
import { Definisjonsliste, LabeledDataElementList } from "../common/Definisjonsliste";
import { addDuration, formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";

interface OppsummeringStepProps {
  innsendingsInformasjon: Array<LabeledDataElement>;
  formState: OpprettKravFormState;
  errors: FieldError[];
  goToPreviousStep: () => void;
  onSubmit: (bekreftelse: boolean) => void;
  isSubmitting: boolean;
}

export default function OppsummeringStep({
  innsendingsInformasjon,
  formState,
  errors,
  goToPreviousStep,
  onSubmit,
  isSubmitting,
}: OppsummeringStepProps) {
  const acceptedFiles = formState.files.filter((f) => !f.error).map((f) => f.file);
  const [bekreftelse, setBekreftelse] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(bekreftelse);
  };

  return (
    <>
      <Heading level="2" size="large">
        Oppsummering
      </Heading>
      <Box>
        <LabeledDataElementList title="Innsendingsinformasjon" entries={innsendingsInformasjon} />
        <Separator />
        <Definisjonsliste
          title="Utbetaling"
          definitions={[
            {
              key: "Utbetalingsperiode",
              value: formaterPeriode({
                start: formState.periodeStart!,
                slutt: formaterDato(
                  formState.periodeType === PeriodeType.INKLUSIV
                    ? addDuration(formState.periodeSlutt!, { days: 1 })!
                    : formState.periodeSlutt!,
                )!,
              }),
            },
            { key: "Kontonummer", value: formState.kontonummer },
            { key: "KID-nummer", value: formState.kid },
            { key: "Beløp", value: formState.belop },
          ]}
        />
        <Separator />
        <form onSubmit={handleSubmit}>
          <Box marginBlock="space-0 space-32">
            <Heading level="3" size="medium" spacing>
              Vedlegg
            </Heading>
            <VedleggSummary vedlegg={acceptedFiles} />
            <Separator />
            <CheckboxGroup error={errorAt("/bekreftelse", errors)} legend="Bekreftelse">
              <Checkbox
                name="bekreftelse"
                value="bekreftet"
                id="bekreftelse"
                checked={bekreftelse}
                onChange={(e) => setBekreftelse(e.target.checked)}
              >
                {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
              </Checkbox>
            </CheckboxGroup>
          </Box>
          <HStack gap="space-8">
            <Button type="button" variant="tertiary" onClick={goToPreviousStep}>
              Tilbake
            </Button>
            <Button type="submit" loading={isSubmitting}>
              Bekreft og send inn
            </Button>
          </HStack>
        </form>
      </Box>
    </>
  );
}
