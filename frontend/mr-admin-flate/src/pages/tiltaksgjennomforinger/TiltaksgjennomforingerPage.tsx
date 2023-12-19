import { useTitle } from "mulighetsrommet-frontend-common";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { gjennomforingPaginationAtom, tiltaksgjennomforingfilterAtom } from "../../api/atoms";
import { FilterAndTableLayout } from "../../components/filter/FilterAndTableLayout";
import { TiltaksgjennomforingFilterButtons } from "../../components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingFilterTags } from "../../components/filter/TiltaksgjennomforingFilterTags";
import { TiltaksgjennomforingFilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import { ReloadAppErrorBoundary } from "../../ErrorBoundary";
import { useAtom } from "jotai";

export function TiltaksgjennomforingerPage() {
  useTitle("Tiltaksgjennomføringer");

  const [page] = useAtom(gjennomforingPaginationAtom);
  const [filter] = useAtom(tiltaksgjennomforingfilterAtom);
  const { data, isLoading } = useAdminTiltaksgjennomforinger(filter, page);

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
                <TiltaksgjennomforingsTabell
                  isLoading={isLoading}
                  paginerteTiltaksgjennomforinger={data}
                />
              </ReloadAppErrorBoundary>
            }
          />
        </ContainerLayout>
      </MainContainer>
    </>
  );
}
