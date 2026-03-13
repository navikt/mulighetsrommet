import { useNavigate } from "react-router";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useRedigerUtbetalingForm } from "@/components/utbetaling/form/useRedigerUtbetalingForm";
import { FormProvider } from "react-hook-form";
import { UtbetalingForm } from "@/components/utbetaling/form/UtbetalingForm";
import { useUtbetaling } from "@/pages/gjennomforing/utbetaling/utbetalingPageLoader";
import { KorreksjonInfoCard } from "@/components/utbetaling/KorreksjonInfoCard";

export function RedigerUtbetalingPage() {
  const navigate = useNavigate();

  const { gjennomforingId, utbetalingId } = useRequiredParams(["gjennomforingId", "utbetalingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);

  function navigateToUtbetaling() {
    navigate(`/gjennomforinger/${gjennomforingId}/utbetalinger/${utbetalingId}`);
  }

  const { utbetaling } = useUtbetaling(utbetalingId);
  const { form, submit } = useRedigerUtbetalingForm(utbetaling, {
    onSuccess: navigateToUtbetaling,
  });

  const formId = "opprett-utbetaling";
  return (
    <VStack gap="space-24" padding="space-16" className="rounded-lg border-ax-neutral-400 border">
      <Heading size="medium" level="2">
        Rediger utbetaling
      </Heading>

      {utbetaling.korreksjon?.opprinneligUtbetaling && (
        <KorreksjonInfoCard utbetalingId={utbetaling.korreksjon.opprinneligUtbetaling} />
      )}

      <FormProvider {...form}>
        <UtbetalingForm
          id={formId}
          onSubmit={form.handleSubmit(submit)}
          arrangorId={gjennomforing.arrangor.id}
          startDato={gjennomforing.startDato}
        />
      </FormProvider>

      <HStack align="start" justify="end" gap="space-8">
        <Button size="small" variant="tertiary" onClick={navigateToUtbetaling}>
          Avbryt
        </Button>
        <Button size="small" type="submit" form={formId}>
          Rediger
        </Button>
      </HStack>
    </VStack>
  );
}
