import { ContainerLayout } from "@/layouts/ContainerLayout";
import { MainContainer } from "@/layouts/MainContainer";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { ReloadAppErrorBoundary, useTitle } from "@mr/frontend-common";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Skeleton } from "@navikt/ds-react";
import { Suspense } from "react";
import { TiltakstypeTabell } from "@/components/tabell/TiltakstypeTabell";

export function TiltakstyperPage() {
  useTitle("Tiltakstyper");
  return (
    <>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Forside", lenke: "/" },
          { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
        ]}
      />
      <HeaderBanner heading="Oversikt over tiltakstyper" ikon={<TiltakstypeIkon />} />
      <MainContainer>
        <ContainerLayout>
          <ReloadAppErrorBoundary>
            <Suspense fallback={<Skeleton height={500} variant="rounded" />}>
              <TiltakstypeTabell />
            </Suspense>
          </ReloadAppErrorBoundary>
        </ContainerLayout>
      </MainContainer>
      <TilToppenKnapp />
    </>
  );
}
