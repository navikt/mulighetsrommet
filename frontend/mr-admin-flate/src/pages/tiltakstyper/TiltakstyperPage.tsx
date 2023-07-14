import { Tiltakstypefilter } from "../../components/filter/Tiltakstypefilter";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { TiltakstypeTabell } from "../../components/tabell/TiltakstypeTabell";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";
import { HeaderBanner } from "../../layouts/HeaderBanner";

export function TiltakstyperPage() {
  return (
    <>
      <ErrorBoundary FallbackComponent={ErrorFallback}>
        <HeaderBanner heading="Oversikt over tiltakstyper" />
        <MainContainer>
          <ContainerLayoutOversikt>
            <Tiltakstypefilter />
            <TiltakstypeTabell />
          </ContainerLayoutOversikt>
        </MainContainer>
      </ErrorBoundary>
    </>
  );
}
