import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useArrangorBetalingsinformasjon } from "@/api/arrangor/useArrangorBetalingsinformasjon";
import { Heading, VStack } from "@navikt/ds-react";

export function OpprettUtbetalingAnskaffelsePage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, prismodell } = useGjennomforing(gjennomforingId);
  const { data: betalingsinformasjon } = useArrangorBetalingsinformasjon(gjennomforing.arrangor.id);

  return (
    <VStack gap="space-24" padding="space-16" className="rounded-lg border-ax-neutral-400 border">
      <Heading size="medium" level="2">
        Opprett utbetaling for anskaffelse
      </Heading>
      <OpprettUtbetalingForm
        gjennomforing={gjennomforing}
        prismodell={prismodell}
        betalingsinformasjon={betalingsinformasjon}
      />
    </VStack>
  );
}
