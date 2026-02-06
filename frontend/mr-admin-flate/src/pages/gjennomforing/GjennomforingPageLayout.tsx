import { InlineErrorBoundary } from "@/ErrorBoundary";
import { VStack } from "@navikt/ds-react";
import { GjennomforingKnapperad } from "./GjennomforingKnapperad";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useGjennomforing, useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

function useGjennomforingInfoData() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, veilederinfo } = useGjennomforing(gjennomforingId);
  const handlinger = useGjennomforingHandlinger(gjennomforing.id);
  const { data: ansatt } = useHentAnsatt();

  return {
    gjennomforing,
    veilederinfo,
    handlinger,
    ansatt,
  };
}

export function GjennomforingPageLayout({ children }: { children: React.ReactNode }) {
  const { gjennomforing, veilederinfo, handlinger, ansatt } = useGjennomforingInfoData();
  return (
    <InlineErrorBoundary>
      <VStack className="pb-6">
        <GjennomforingKnapperad
          ansatt={ansatt}
          gjennomforing={gjennomforing}
          veilederinfo={veilederinfo}
          handlinger={handlinger}
        />
        <Separator />
        {children}
      </VStack>
    </InlineErrorBoundary>
  );
}
