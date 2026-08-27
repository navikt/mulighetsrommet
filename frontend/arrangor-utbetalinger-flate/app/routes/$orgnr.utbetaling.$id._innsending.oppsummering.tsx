import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Box, Checkbox, CheckboxGroup, ErrorSummary, Heading } from "@navikt/ds-react";
import { FieldError } from "@arrangor-utbetalinger/api-client";
import { SubmitEvent, useEffect, useRef, useState } from "react";
import { MetaFunction, useLocation, useNavigate } from "react-router";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { tekster } from "~/tekster";
import { pathTo, useIdFromUrl, useOrgnrFromUrl } from "~/utils/navigation";
import { errorAt } from "~/utils/validering";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useGodkjennUtbetaling } from "~/hooks/useGodkjennUtbetaling";
import { useUtbetalingWizard } from "~/hooks/useUtbetalingWizard";
import { BlokkeringerVarsler } from "~/components/common/BlokkeringerVarsler";
import { StepFooter } from "~/components/utbetaling/StepFooter";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 4 av 4: Oppsummering - Godkjenn innsending" },
    {
      name: "description",
      content: "Oppsummering av innsendingen og betalingsinformasjon",
    },
  ];
};

export default function BekreftUtbetaling() {
  const id = useIdFromUrl();
  const orgnr = useOrgnrFromUrl();
  const navigate = useNavigate();
  const { updatedAt, kid } = useLocation().state || {};

  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const godkjennUtbetaling = useGodkjennUtbetaling();

  const wizard = useUtbetalingWizard(utbetaling);

  const [bekreftelse, setBekreftelse] = useState(false);
  const [errors, setErrors] = useState<FieldError[]>([]);

  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const hasError = errors.length > 0;

  const submit = async () => {
    const newErrors: FieldError[] = [];

    if (!bekreftelse) {
      newErrors.push({
        pointer: "/bekreftelse",
        detail: "Du må bekrefte at opplysningene er korrekte",
      });
    }

    if (newErrors.length > 0) {
      setErrors(newErrors);
      return;
    }

    const result = await godkjennUtbetaling.mutateAsync({
      id: id,
      updatedAt: updatedAt,
      kid: kid || null,
    });

    if (result.errors) {
      setErrors(result.errors);
    } else if (result.success) {
      navigate(pathTo.kvittering(orgnr, id));
    }
  };

  const handleSubmit = async (e: SubmitEvent) => {
    e.preventDefault();
    await submit();
  };

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [hasError]);

  return (
    <>
      <Heading level="2" spacing size="large">
        Oppsummering
      </Heading>
      <Definisjonsliste
        title="Innsendingsinformasjon"
        definitions={[
          {
            key: "Arrangør",
            value: `${utbetaling.arrangor.navn} - ${utbetaling.arrangor.organisasjonsnummer}`,
          },
          {
            key: "Tiltaksnavn",
            value: `${utbetaling.gjennomforing.navn} (${utbetaling.gjennomforing.lopenummer})`,
          },
          { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
          {
            key: "Utbetalingsperiode",
            value: formaterPeriode(utbetaling.periode),
          },
        ]}
      />
      <Separator />
      <SatsPerioderOgBelop
        pris={utbetaling.beregning.pris}
        satsDetaljer={utbetaling.beregning.satsDetaljer}
      />
      <Separator />
      <Definisjonsliste
        title="Betalingsinformasjon"
        definitions={[
          {
            key: "Kontonummer",
            value: utbetaling.betalingsinformasjon?.kontonummer ?? "Kontonummer mangler",
          },
          { key: "KID-nummer", value: kid || "-" },
        ]}
      />
      <Separator />
      <form onSubmit={handleSubmit}>
        <Box marginBlock="space-0 space-16">
          <Heading size="medium" level="3">
            Bekreftelse
          </Heading>
          <CheckboxGroup error={errorAt("/bekreftelse", errors)} hideLegend legend="Bekreftelse">
            <Checkbox
              name="bekreftelse"
              value="bekreftet"
              id="bekreftelse"
              checked={bekreftelse}
              onChange={(e) => setBekreftelse(e.target.checked)}
              error={errorAt("/bekreftelse", errors) !== undefined}
            >
              {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
            </Checkbox>
            <Separator />
            <BlokkeringerVarsler
              blokkeringer={utbetaling.blokkeringer}
              advarsler={utbetaling.advarsler}
            />
          </CheckboxGroup>
          {hasError && (
            <ErrorSummary ref={errorSummaryRef}>
              {errors.map((error: FieldError) => {
                return (
                  <ErrorSummary.Item
                    href={`#${jsonPointerToFieldPath(error.pointer)}`}
                    key={jsonPointerToFieldPath(error.pointer)}
                  >
                    {error.detail}
                  </ErrorSummary.Item>
                );
              })}
            </ErrorSummary>
          )}
        </Box>
        {utbetaling.blokkeringer.length === 0 && (
          <StepFooter
            wizard={wizard}
            primaryAction={{
              label: "Bekreft og send inn",
              onClick: submit,
              loading: godkjennUtbetaling.isPending,
            }}
          />
        )}
      </form>
    </>
  );
}
