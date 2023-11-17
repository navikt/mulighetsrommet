import { Tiltaksgjennomforingfilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { useTitle } from "mulighetsrommet-frontend-common";
import { useAtom } from "jotai";
import { tiltaksgjennomforingfilter } from "../../api/atoms";

export function TiltaksgjennomforingerPage() {
  useTitle("Tiltaksgjennomføringer");
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilter);
  return (
    <>
      <HeaderBanner heading="Oversikt over tiltaksgjennomføringer" />
      <MainContainer>
        <ContainerLayout>
          <Tiltaksgjennomforingfilter filter={filter} setFilter={setFilter} />
          <ErrorBoundary FallbackComponent={ErrorFallback}>
            <TiltaksgjennomforingsTabell />
          </ErrorBoundary>
        </ContainerLayout>
      </MainContainer>
    </>
  );
}
