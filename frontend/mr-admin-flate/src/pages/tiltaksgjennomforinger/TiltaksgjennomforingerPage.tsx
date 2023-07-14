import { Tiltaksgjennomforingfilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";
import { HeaderBanner } from "../../layouts/HeaderBanner";

export function TiltaksgjennomforingerPage() {
  return (
    <>
      <HeaderBanner heading="Oversikt over tiltaksgjennomføringer" />
      <MainContainer>
        <ContainerLayoutOversikt>
          <Tiltaksgjennomforingfilter />
          <ErrorBoundary FallbackComponent={ErrorFallback}>
            <TiltaksgjennomforingsTabell />
          </ErrorBoundary>
        </ContainerLayoutOversikt>
      </MainContainer>
    </>
  );
}
