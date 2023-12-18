import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { avtaleFilterAtom } from "../../api/atoms";
import { AvtaleFilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { useTitle } from "mulighetsrommet-frontend-common";
import { ReloadAppErrorBoundary } from "../../ErrorBoundary";
import { FilterAndTableLayout } from "src/components/filter/FilterAndTableLayout";
import { AvtaleFilterButtons } from "src/components/filter/AvtaleFilterButtons";
import { AvtaleFilterTags } from "src/components/filter/AvtaleFilterTags";

export function AvtalerPage() {
  useTitle("Avtaler");
  const { data: avtaler, isLoading: avtalerIsLoading } = useAvtaler(avtaleFilterAtom);

  return (
    <>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny />
      <ReloadAppErrorBoundary>
        <MainContainer>
          <ContainerLayout>
            <FilterAndTableLayout
              filter={<AvtaleFilter filterAtom={avtaleFilterAtom} />}
              tags={<AvtaleFilterTags filterAtom={avtaleFilterAtom} />}
              buttons={<AvtaleFilterButtons filterAtom={avtaleFilterAtom} />}
              table={
                <AvtaleTabell
                  isLoading={avtalerIsLoading}
                  paginerteAvtaler={avtaler}
                  avtalefilter={avtaleFilterAtom}
                />
              }
            />
          </ContainerLayout>
        </MainContainer>
      </ReloadAppErrorBoundary>
    </>
  );
}
