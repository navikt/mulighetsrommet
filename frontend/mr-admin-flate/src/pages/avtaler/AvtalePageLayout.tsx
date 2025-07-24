import { Separator } from "@/components/detaljside/Metadata";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { VStack } from "@navikt/ds-react";
import { AvtaleKnapperad } from "./AvtaleKnapperad";
import { AvtaleDto, NavAnsatt } from "@mr/api-client-v2";

export function AvtalePageLayout({
  ansatt,
  avtale,
  children,
}: {
  ansatt: NavAnsatt;
  avtale: AvtaleDto;
  children: React.ReactNode;
}) {
  return (
    <InlineErrorBoundary>
      <VStack className="pb-6">
        <AvtaleKnapperad ansatt={ansatt} avtale={avtale} />
        <Separator />
        {children}
      </VStack>
    </InlineErrorBoundary>
  );
}
