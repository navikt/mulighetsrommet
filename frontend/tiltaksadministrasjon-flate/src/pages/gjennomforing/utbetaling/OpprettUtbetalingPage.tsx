import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import { useOpprettUtbetalingForm } from "@/components/utbetaling/form/useOpprettUtbetalingForm";
import { FormProvider } from "react-hook-form";
import { UtbetalingForm } from "@/components/utbetaling/form/UtbetalingForm";

export function OpprettUtbetalingPage() {
  const navigate = useNavigate();

  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, prismodell } = useGjennomforing(gjennomforingId);

  const { form, submit } = useOpprettUtbetalingForm({
    gjennomforingId: gjennomforing.id,
    pris: { belop: null, valuta: prismodell.valuta },
  });

  const formId = "opprett-utbetaling";
  return (
    <VStack gap="space-24" padding="space-16" className="rounded-lg border-ax-neutral-400 border">
      <Heading size="medium" level="2">
        Opprett utbetaling for anskaffelse
      </Heading>

      <FormProvider {...form}>
        <UtbetalingForm
          id={formId}
          onSubmit={form.handleSubmit(submit)}
          arrangorId={gjennomforing.arrangor.id}
          startDato={gjennomforing.startDato}
        />
      </FormProvider>

      <HStack align="start" justify="end" gap="space-8">
        <Button
          size="small"
          variant="tertiary"
          onClick={() => navigate(`/gjennomforinger/${gjennomforing.id}/utbetalinger`)}
        >
          Avbryt
        </Button>
        <Button size="small" type="submit" form={formId}>
          Opprett
        </Button>
      </HStack>
    </VStack>
  );
}
