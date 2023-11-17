import { useTitle } from "mulighetsrommet-frontend-common";
import { ErrorBoundary } from "react-error-boundary";
import { Tiltaksgjennomforingfilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { ErrorFallback } from "../../main";

export function TiltaksgjennomforingerPage() {
  useTitle("Tiltaksgjennomføringer");
  return (
    <>
      <HeaderBanner heading="Oversikt over tiltaksgjennomføringer" />
      <MainContainer>
        <ContainerLayout>
          <Tiltaksgjennomforingfilter />
          <ErrorBoundary FallbackComponent={ErrorFallback}>
            <TiltaksgjennomforingsTabell />
          </ErrorBoundary>
        </ContainerLayout>
      </MainContainer>
    </>
  );
}
