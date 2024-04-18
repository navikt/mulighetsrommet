import { useTitle } from "mulighetsrommet-frontend-common";
import { TiltaksgjennomforingsTabell } from "@/components/tabell/TiltaksgjennomforingsTabell";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { MainContainer } from "@/layouts/MainContainer";
import { tiltaksgjennomforingfilterAtom } from "@/api/atoms";
import { FilterAndTableLayout } from "mulighetsrommet-frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TiltaksgjennomforingFilterButtons } from "@/components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingFiltertags } from "@/components/filter/TiltaksgjennomforingFiltertags";
import { TiltaksgjennomforingFilter } from "@/components/filter/TiltaksgjennomforingFilter";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { TilToppenKnapp } from "mulighetsrommet-frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { useState } from "react";
import { NullstillKnappForTiltaksgjennomforinger } from "@/pages/tiltaksgjennomforinger/NullstillKnappForTiltaksgjennomforinger";

export function TiltaksgjennomforingerPage() {
  useTitle("Tiltaksgjennomføringer");
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const [tagsHeight, setTagsHeight] = useState(0);

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
