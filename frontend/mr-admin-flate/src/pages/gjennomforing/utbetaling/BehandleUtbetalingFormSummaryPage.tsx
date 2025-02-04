import { FormSummary } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { InferredUtbetalingSchema } from "../../../components/utbetaling/UtbetalingSchema";
import { formaterDato } from "../../../utils/Utils";

export function BehandleUtbetalingFormSummaryPage() {
  const { watch } = useFormContext<InferredUtbetalingSchema>();

  return (
    <div className="prose">
      <FormSummary>
        <FormSummary.Header>
          <FormSummary.Heading level="2">Oppsummering</FormSummary.Heading>
        </FormSummary.Header>

        <FormSummary.Answers>
          <FormSummary.Answer>
            <FormSummary.Label>1. Utbetalingsinformasjon</FormSummary.Label>
            <FormSummary.Value>
              <FormSummary.Answers>
                <FormSummary.Answer>
                  <FormSummary.Label>Periodestart</FormSummary.Label>
                  <FormSummary.Value>
                    {formaterDato(watch("arrangorinfo.periode.start"))}
                  </FormSummary.Value>
                </FormSummary.Answer>
                <FormSummary.Answer>
                  <FormSummary.Label>Periodeslutt</FormSummary.Label>
                  <FormSummary.Value>
                    {formaterDato(watch("arrangorinfo.periode.slutt"))}
                  </FormSummary.Value>
                </FormSummary.Answer>
                <FormSummary.Answer>
                  <FormSummary.Label>Beskrivelse</FormSummary.Label>
                  <FormSummary.Value>{watch("arrangorinfo.beskrivelse")}</FormSummary.Value>
                </FormSummary.Answer>
                <FormSummary.Answer>
                  <FormSummary.Label>Kontonummer</FormSummary.Label>
                  <FormSummary.Value>{watch("arrangorinfo.kontonummer")}</FormSummary.Value>
                </FormSummary.Answer>
                <FormSummary.Answer>
                  <FormSummary.Label>Valgfritt KID-nummer</FormSummary.Label>
                  <FormSummary.Value>
                    {watch("arrangorinfo.kidNummer") || "KID-nummer er ikke oppgitt"}
                  </FormSummary.Value>
                </FormSummary.Answer>
                <FormSummary.Answer>
                  <FormSummary.Label>Beløp</FormSummary.Label>
                  <FormSummary.Value>{watch("arrangorinfo.belop")}</FormSummary.Value>
                </FormSummary.Answer>
              </FormSummary.Answers>
            </FormSummary.Value>
          </FormSummary.Answer>
        </FormSummary.Answers>
      </FormSummary>
    </div>
  );
}
