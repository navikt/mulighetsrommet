import { Separator } from "@/components/detaljside/Metadata";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { VStack } from "@navikt/ds-react";
import { GjennomforingKnapperad } from "./GjennomforingKnapperad";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useParams } from "react-router";

function useGjennomforingInfoData() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: ansatt } = useHentAnsatt();
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);

  return {
    gjennomforing,
    ansatt,
    avtale,
  };
}

export function GjennomforingPageLayout({ children }: { children: React.ReactNode }) {
  const { gjennomforing, avtale, ansatt } = useGjennomforingInfoData();
  return (
    <InlineErrorBoundary>
      <VStack className="pb-6">
        <GjennomforingKnapperad ansatt={ansatt} gjennomforing={gjennomforing} avtale={avtale} />
        <Separator />
        {children}
      </VStack>
    </InlineErrorBoundary>
  );
}
