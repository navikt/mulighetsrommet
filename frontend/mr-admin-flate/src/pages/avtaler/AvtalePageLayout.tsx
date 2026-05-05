import { InlineErrorBoundary } from "@/ErrorBoundary";
import { VStack } from "@navikt/ds-react";
import { AvtaleHandlinger } from "./AvtaleHandlinger";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { AvtaleDto } from "@tiltaksadministrasjon/api-client";
import { ReactNode } from "react";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

export function AvtalePageLayout({ avtale, children }: { avtale: AvtaleDto; children: ReactNode }) {
  return (
    <WhitePaddedBox>
      <InlineErrorBoundary>
        <VStack className="pb-6">
          <AvtaleHandlinger avtale={avtale} />
          <Separator />
          {children}
        </VStack>
      </InlineErrorBoundary>
    </WhitePaddedBox>
  );
}
