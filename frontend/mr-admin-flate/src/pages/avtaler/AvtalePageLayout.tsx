import { Separator } from "@/components/detaljside/Metadata";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { VStack } from "@navikt/ds-react";
import { AvtaleKnapperad } from "./AvtaleKnapperad";
import { AvtaleDto } from "@mr/api-client-v2";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function AvtalePageLayout({
  avtale,
  children,
}: {
  avtale: AvtaleDto;
  children: React.ReactNode;
}) {
  return (
    <ContentBox>
      <WhitePaddedBox>
        <InlineErrorBoundary>
          <VStack className="pb-6">
            <AvtaleKnapperad avtale={avtale} />
            <Separator />
            {children}
          </VStack>
        </InlineErrorBoundary>
      </WhitePaddedBox>
    </ContentBox>
  );
}
