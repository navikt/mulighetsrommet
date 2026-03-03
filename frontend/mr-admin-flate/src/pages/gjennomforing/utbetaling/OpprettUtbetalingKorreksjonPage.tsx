import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useArrangorBetalingsinformasjon } from "@/api/arrangor/useArrangorBetalingsinformasjon";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Heading, VStack } from "@navikt/ds-react";

export function OpprettUtbetalingKorreksjonPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, prismodell } = useGjennomforing(gjennomforingId);
  const { data: betalingsinformasjon } = useArrangorBetalingsinformasjon(gjennomforing.arrangor.id);

  return (
    <div className="flex flex-col gap-4">
      <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
      <VStack gap="space-24" padding="space-16" className="rounded-lg border-ax-neutral-400 border">
        <Heading size="medium" level="2">
          Opprett korreksjon
        </Heading>
        <OpprettUtbetalingForm
          gjennomforing={gjennomforing}
          prismodell={prismodell}
          betalingsinformasjon={betalingsinformasjon}
        />
      </VStack>
    </div>
  );
}
