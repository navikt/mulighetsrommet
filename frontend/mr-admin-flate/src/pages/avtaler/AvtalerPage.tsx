import { avtaleFilterAtom, AvtaleFilterSchema } from "@/api/atoms";
import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { MainContainer } from "@/layouts/MainContainer";
import { LagredeFilterOversikt, ReloadAppErrorBoundary, useTitle } from "@mr/frontend-common";
import { AvtaleFiltertags } from "@/components/filter/AvtaleFiltertags";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { useState } from "react";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { NullstillKnappForAvtaler } from "@/pages/avtaler/NullstillKnappForAvtaler";
import { LagretDokumenttype } from "@mr/api-client";
import { useAtom } from "jotai/index";

export function AvtalerPage() {
  useTitle("Avtaler");
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const [tagsHeight, setTagsHeight] = useState(0);

  const [filter, setFilter] = useAtom(avtaleFilterAtom);

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
              nullstillFilterButton={<NullstillKnappForAvtaler filterAtom={avtaleFilterAtom} />}
              filter={<AvtaleFilter filterAtom={avtaleFilterAtom} />}
              lagredeFilter={
                <LagredeFilterOversikt
                  setFilter={setFilter}
                  filter={filter}
                  dokumenttype={LagretDokumenttype.AVTALE}
                  validateFilterStructure={(filter) => {
                    return AvtaleFilterSchema.safeParse(filter).success;
                  }}
                />
              }
              tags={
                <AvtaleFiltertags
                  filterAtom={avtaleFilterAtom}
                  filterOpen={filterOpen}
                  setTagsHeight={setTagsHeight}
                />
              }
              buttons={<AvtaleFilterButtons />}
              table={
                <AvtaleTabell
                  filterAtom={avtaleFilterAtom}
                  tagsHeight={tagsHeight}
                  filterOpen={filterOpen}
                />
              }
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
