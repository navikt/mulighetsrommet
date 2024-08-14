import { LagredeFilterOversikt, ReloadAppErrorBoundary, useTitle } from "@mr/frontend-common";
import { TiltaksgjennomforingsTabell } from "@/components/tabell/TiltaksgjennomforingsTabell";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { MainContainer } from "@/layouts/MainContainer";
import { tiltaksgjennomforingfilterAtom, TiltaksgjennomforingFilterSchema } from "@/api/atoms";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TiltaksgjennomforingFilterButtons } from "@/components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingFiltertags } from "@/components/filter/TiltaksgjennomforingFiltertags";
import { TiltaksgjennomforingFilter } from "@/components/filter/TiltaksgjennomforingFilter";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { useState } from "react";
import { NullstillKnappForTiltaksgjennomforinger } from "@/pages/tiltaksgjennomforinger/NullstillKnappForTiltaksgjennomforinger";
import { LagretDokumenttype } from "@mr/api-client";
import { useAtom } from "jotai/index";

export function TiltaksgjennomforingerPage() {
  useTitle("Tiltaksgjennomføringer");
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilterAtom);

  return (
    <>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Forside", lenke: "/" },
          { tittel: "Tiltaksgjennomføringer", lenke: "/tiltaksgjennomforinger" },
        ]}
      />
      <HeaderBanner
        heading="Oversikt over tiltaksgjennomføringer"
        ikon={<TiltaksgjennomforingIkon />}
      />
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
