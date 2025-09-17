import { Separator } from "@/components/detaljside/Metadata";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { VStack } from "@navikt/ds-react";
import { GjennomforingKnapperad } from "./GjennomforingKnapperad";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useRequiredParams } from "@/hooks/useRequiredParams";

function useGjennomforingInfoData() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  const { data: ansatt } = useHentAnsatt();

  return {
    gjennomforing,
    ansatt,
  };
}

export function GjennomforingPageLayout({ children }: { children: React.ReactNode }) {
  const { gjennomforing, ansatt } = useGjennomforingInfoData();
  return (
    <InlineErrorBoundary>
      <VStack className="pb-6">
        <GjennomforingKnapperad ansatt={ansatt} gjennomforing={gjennomforing} />
        <Separator />
        {children}
      </VStack>
    </InlineErrorBoundary>
  );
}
