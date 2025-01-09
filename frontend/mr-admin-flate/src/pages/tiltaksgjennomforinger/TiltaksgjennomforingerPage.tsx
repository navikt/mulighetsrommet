import { tiltaksgjennomforingfilterAtom, TiltaksgjennomforingFilterSchema } from "@/api/atoms";
import { TiltaksgjennomforingFilter } from "@/components/filter/TiltaksgjennomforingFilter";
import { TiltaksgjennomforingFilterButtons } from "@/components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingFiltertags } from "@/components/filter/TiltaksgjennomforingFiltertags";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { TiltaksgjennomforingsTabell } from "@/components/tabell/TiltaksgjennomforingsTabell";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { MainContainer } from "@/layouts/MainContainer";
import { NullstillKnappForTiltaksgjennomforinger } from "@/pages/tiltaksgjennomforinger/NullstillKnappForTiltaksgjennomforinger";
import { LagretDokumenttype } from "@mr/api-client";
import { LagredeFilterOversikt, useOpenFilterWhenThreshold, useTitle } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useAtom } from "jotai/index";
import { useState } from "react";

export function TiltaksgjennomforingerPage() {
  useTitle("Gjennomføringer");
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilterAtom);

  return (
    <>
      <HeaderBanner heading="Oversikt over gjennomføringer" ikon={<TiltaksgjennomforingIkon />} />
      <MainContainer>
        <ContainerLayout>
          <FilterAndTableLayout
            filter={<TiltaksgjennomforingFilter filterAtom={tiltaksgjennomforingfilterAtom} />}
            lagredeFilter={
              <LagredeFilterOversikt
                setFilter={setFilter}
                filter={filter}
                dokumenttype={LagretDokumenttype.TILTAKSGJENNOMFØRING}
                validateFilterStructure={(filter) => {
                  return TiltaksgjennomforingFilterSchema.safeParse(filter).success;
                }}
              />
            }
            tags={
              <TiltaksgjennomforingFiltertags
                filterAtom={tiltaksgjennomforingfilterAtom}
                filterOpen={filterOpen}
                setTagsHeight={setTagsHeight}
              />
            }
            buttons={<TiltaksgjennomforingFilterButtons />}
            table={
              <ReloadAppErrorBoundary>
                <TiltaksgjennomforingsTabell
                  filterAtom={tiltaksgjennomforingfilterAtom}
                  tagsHeight={tagsHeight}
                  filterOpen={filterOpen}
                />
              </ReloadAppErrorBoundary>
            }
            filterOpen={filterOpen}
            setFilterOpen={setFilterOpen}
            nullstillFilterButton={
              <NullstillKnappForTiltaksgjennomforinger
                filterAtom={tiltaksgjennomforingfilterAtom}
              />
            }
          />
        </ContainerLayout>
      </MainContainer>
      <TilToppenKnapp />
    </>
  );
}
