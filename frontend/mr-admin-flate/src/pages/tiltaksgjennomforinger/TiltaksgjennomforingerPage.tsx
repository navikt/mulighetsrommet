import { useTitle } from "mulighetsrommet-frontend-common";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { tiltaksgjennomforingfilterAtom } from "../../api/atoms";
import { FilterAndTableLayout } from "../../components/filter/FilterAndTableLayout";
import { TiltaksgjennomforingFilterButtons } from "../../components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingFilterTags } from "../../components/filter/TiltaksgjennomforingFilterTags";
import { TiltaksgjennomforingFilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { ReloadAppErrorBoundary } from "../../ErrorBoundary";
import { TilToppenKnapp } from "../../../../frontend-common/components/tilToppenKnapp/TilToppenKnapp";

export function TiltaksgjennomforingerPage() {
  useTitle("Tiltaksgjennomføringer");

  return (
    <>
      <HeaderBanner heading="Oversikt over tiltaksgjennomføringer" />
      <MainContainer>
        <ContainerLayout>
          <FilterAndTableLayout
            filter={<TiltaksgjennomforingFilter filterAtom={tiltaksgjennomforingfilterAtom} />}
            tags={<TiltaksgjennomforingFilterTags filterAtom={tiltaksgjennomforingfilterAtom} />}
            buttons={
              <TiltaksgjennomforingFilterButtons filterAtom={tiltaksgjennomforingfilterAtom} />
            }
            table={
              <ReloadAppErrorBoundary>
                <TiltaksgjennomforingsTabell filterAtom={tiltaksgjennomforingfilterAtom} />
              </ReloadAppErrorBoundary>
            }
          />
        </ContainerLayout>
      </MainContainer>
      <TilToppenKnapp />
    </>
  );
}
