import { Tiltakstypefilter } from "../../components/filter/Tiltakstypefilter";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { TiltakstypeTabell } from "../../components/tabell/TiltakstypeTabell";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { useTitle } from "mulighetsrommet-frontend-common";

export function TiltakstyperPage() {
  useTitle("Tiltakstyper");
  return (
    <>
      <HeaderBanner heading="Oversikt over tiltakstyper" />
      <MainContainer>
        <ContainerLayout>
          <Tiltakstypefilter />
          <ErrorBoundary FallbackComponent={ErrorFallback}>
            <TiltakstypeTabell />
          </ErrorBoundary>
        </ContainerLayout>
      </MainContainer>
    </>
  );
}
