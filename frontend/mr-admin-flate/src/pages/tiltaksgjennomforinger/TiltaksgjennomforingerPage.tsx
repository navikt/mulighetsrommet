import { useTitle } from "mulighetsrommet-frontend-common";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { tiltaksgjennomforingfilterAtom } from "@/api/atoms";
import { FilterAndTableLayout } from "../../components/filter/FilterAndTableLayout";
import { TiltaksgjennomforingFilterButtons } from "../../components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingFiltertags } from "../../components/filter/TiltaksgjennomforingFiltertags";
import { TiltaksgjennomforingFilter } from "../../components/filter/TiltaksgjennomforingFilter";
import { ReloadAppErrorBoundary } from "../../ErrorBoundary";
import { TilToppenKnapp } from "mulighetsrommet-frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Brodsmuler } from "../../components/navigering/Brodsmuler";
import { TiltaksgjennomforingIkon } from "../../components/ikoner/TiltaksgjennomforingIkon";
import { useState } from "react";

export function TiltaksgjennomforingerPage() {
  useTitle("Tiltaksgjennomføringer");
  const [filterOpen, setFilterOpen] = useState<boolean>(true);

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
              />
            }
            buttons={
              <TiltaksgjennomforingFilterButtons filterAtom={tiltaksgjennomforingfilterAtom} />
            }
            table={
              <ReloadAppErrorBoundary>
                <TiltaksgjennomforingsTabell filterAtom={tiltaksgjennomforingfilterAtom} />
              </ReloadAppErrorBoundary>
            }
            filterOpen={filterOpen}
            setFilterOpen={setFilterOpen}
          />
        </ContainerLayout>
      </MainContainer>
      <TilToppenKnapp />
    </>
  );
}
