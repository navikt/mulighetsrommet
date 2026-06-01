import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { TiltakstypeTable } from "@/components/tiltakstype/TiltakstypeTable";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Skeleton } from "@navikt/ds-react";
import { Suspense } from "react";

export function TiltakstyperPage() {
  return (
    <>
      <title>Tiltakstyper</title>
      <HeaderBanner heading="Oversikt over tiltakstyper" ikon={<TiltakstypeIkon />} />
      <ContentBox>
        <ReloadAppErrorBoundary>
          <Suspense fallback={<Skeleton height={500} variant="rounded" />}>
            <TiltakstypeTable />
          </Suspense>
        </ReloadAppErrorBoundary>
      </ContentBox>
      <TilToppenKnapp />
    </>
  );
}
