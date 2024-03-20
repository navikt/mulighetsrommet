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
import { AvtaleFiltertags } from "../../components/filter/AvtaleFiltertags";
import { TilToppenKnapp } from "mulighetsrommet-frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Brodsmuler } from "../../components/navigering/Brodsmuler";
import { AvtaleIkon } from "../../components/ikoner/AvtaleIkon";
import { useState } from "react";

export function AvtalerPage() {
  useTitle("Avtaler");
  const [filterOpen, setFilterOpen] = useState<boolean>(true);

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
              tags={<AvtaleFiltertags filterAtom={avtaleFilterAtom} filterOpen={filterOpen} />}
              buttons={<AvtaleFilterButtons filterAtom={avtaleFilterAtom} />}
              table={<AvtaleTabell filterAtom={avtaleFilterAtom} />}
              setFilterOpen={setFilterOpen}
              filterOpen={filterOpen}
            />
          </ContainerLayout>
        </MainContainer>
      </ReloadAppErrorBoundary>
      <TilToppenKnapp />
    </>
  );
}
