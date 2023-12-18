import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { avtaleFilterAtom } from "../../api/atoms";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { useTitle } from "mulighetsrommet-frontend-common";
import { ReloadAppErrorBoundary } from "../../ErrorBoundary";

export function AvtalerPage() {
  useTitle("Avtaler");
  const { data: avtaler, isLoading: avtalerIsLoading } = useAvtaler(avtaleFilterAtom);

  return (
    <>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny />
      <ReloadAppErrorBoundary>
        <MainContainer>
          <ContainerLayout>
            <Avtalefilter filterAtom={avtaleFilterAtom} />
            <AvtaleTabell
              isLoading={avtalerIsLoading}
              paginerteAvtaler={avtaler}
              avtalefilter={avtaleFilterAtom}
            />
          </ContainerLayout>
        </MainContainer>
      </ReloadAppErrorBoundary>
    </>
  );
}
