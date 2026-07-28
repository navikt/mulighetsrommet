import { InlineErrorBoundary } from "@/ErrorBoundary";
import { VStack } from "@navikt/ds-react";
import { GjennomforingAvtaleHandlinger } from "./GjennomforingAvtaleHandlinger";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useGjennomforing, useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import {
  isGjennomforingAvtaleDetaljer,
  isGjennomforingEnkeltplassDetaljer,
} from "@/api/gjennomforing/utils";
import { GjennomforingEnkeltplassHandlinger } from "@/pages/gjennomforing/GjennomforingEnkeltplassHandlinger";
import { ReactNode } from "react";

export function GjennomforingPageLayout({ children }: { children: ReactNode }) {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);

  const { data: ansatt } = useHentAnsatt();
  const detaljer = useGjennomforing(gjennomforingId);
  const handlinger = useGjennomforingHandlinger(gjennomforingId);

  return (
    <InlineErrorBoundary>
      <VStack className="pb-6">
        {isGjennomforingAvtaleDetaljer(detaljer) ? (
          <GjennomforingAvtaleHandlinger
            ansatt={ansatt}
            gjennomforing={detaljer.gjennomforing}
            veilederinfo={detaljer.veilederinfo}
            handlinger={handlinger}
          />
        ) : isGjennomforingEnkeltplassDetaljer(detaljer) ? (
          <GjennomforingEnkeltplassHandlinger gjennomforing={detaljer.gjennomforing} />
        ) : null}
        <Separator />
        {children}
      </VStack>
    </InlineErrorBoundary>
  );
}
