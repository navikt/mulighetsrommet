import { ErrorBoundary } from "react-error-boundary";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { ErrorFallback } from "../../main";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { useTitle } from "mulighetsrommet-frontend-common";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { avtaleFilterAtom } from "../../api/atoms";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";

export function AvtalerPage() {
  useTitle("Avtaler");
  const { data: avtaler, isLoading: avtalerIsLoading } = useAvtaler(avtaleFilterAtom);

  return (
    <>
      <HeaderBanner heading="Oversikt over avtaler" />
      <MainContainer>
        <ContainerLayout>
          <Avtalefilter filterAtom={avtaleFilterAtom} />
          <ErrorBoundary FallbackComponent={ErrorFallback}>
            <AvtaleTabell
              isLoading={avtalerIsLoading}
              paginerteAvtaler={avtaler}
              avtalefilter={avtaleFilterAtom}
            />
          </ErrorBoundary>
        </ContainerLayout>
      </MainContainer>
    </>
  );
}
