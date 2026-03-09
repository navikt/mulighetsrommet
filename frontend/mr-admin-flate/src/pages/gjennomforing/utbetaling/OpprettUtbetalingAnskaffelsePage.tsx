import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { OpprettUtbetalingAnskaffelseForm } from "@/components/utbetaling/OpprettUtbetalingAnskaffelseForm";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Heading, VStack } from "@navikt/ds-react";

export function OpprettUtbetalingAnskaffelsePage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, prismodell } = useGjennomforing(gjennomforingId);

  return (
    <VStack gap="space-24" padding="space-16" className="rounded-lg border-ax-neutral-400 border">
      <Heading size="medium" level="2">
        Opprett utbetaling for anskaffelse
      </Heading>
      <OpprettUtbetalingAnskaffelseForm gjennomforing={gjennomforing} prismodell={prismodell} />
    </VStack>
  );
}
