import { avtaleFilterAtom } from "../../api/atoms";
import { AvtaleFilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { useTitle } from "mulighetsrommet-frontend-common";
import { ReloadAppErrorBoundary } from "../../ErrorBoundary";
import { FilterAndTableLayout } from "../../components/filter/FilterAndTableLayout";
import { AvtaleFilterButtons } from "../../components/filter/AvtaleFilterButtons";
import { AvtaleFilterTags } from "../../components/filter/AvtaleFilterTags";
import { TilToppenKnapp } from "../../../../frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Brodsmuler } from "../../components/navigering/Brodsmuler";
import { AvtaleIkon } from "../../components/ikoner/AvtaleIkon";

export function AvtalerPage() {
  useTitle("Avtaler");

  return (
    <>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Forside", lenke: "/" },
          { tittel: "Avtaler", lenke: "/avtaler" },
        ]}
      />
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny ikon={<AvtaleIkon />} />
      <ReloadAppErrorBoundary>
        <MainContainer>
          <ContainerLayout>
            <FilterAndTableLayout
              filter={<AvtaleFilter filterAtom={avtaleFilterAtom} />}
              tags={<AvtaleFilterTags filterAtom={avtaleFilterAtom} />}
              buttons={<AvtaleFilterButtons filterAtom={avtaleFilterAtom} />}
              table={<AvtaleTabell filterAtom={avtaleFilterAtom} />}
            />
          </ContainerLayout>
        </MainContainer>
      </ReloadAppErrorBoundary>
      <TilToppenKnapp />
    </>
  );
}
