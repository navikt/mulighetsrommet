import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { TiltakstypeTabell } from "../../components/tabell/TiltakstypeTabell";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { useTitle } from "mulighetsrommet-frontend-common";
import { ReloadAppErrorBoundary } from "../../ErrorBoundary";
import { TilToppenKnapp } from "../../../../frontend-common/components/tilToppenKnapp/TilToppenKnapp";

export function TiltakstyperPage() {
  useTitle("Tiltakstyper");
  return (
    <>
      <HeaderBanner heading="Oversikt over tiltakstyper" />
      <MainContainer>
        <ContainerLayout>
          <ReloadAppErrorBoundary>
            <TiltakstypeTabell />
          </ReloadAppErrorBoundary>
        </ContainerLayout>
      </MainContainer>
      <TilToppenKnapp />
    </>
  );
}
