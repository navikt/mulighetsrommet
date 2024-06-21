import { TiltakLoader } from "@/components/TiltakLoader";
import { FilterAndTableLayout } from "mulighetsrommet-frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { Tiltaksgjennomforingsoversikt } from "@/components/oversikt/Tiltaksgjennomforingsoversikt";
import { useNavTiltaksgjennomforinger } from "@/api/queries/useTiltaksgjennomforinger";
import {
  isFilterReady,
  useArbeidsmarkedstiltakFilterValue,
  useResetArbeidsmarkedstiltakFilterUtenBrukerIKontekst,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { NavFiltertags } from "@/apps/nav/filtrering/NavFiltertags";
import { Suspense, useState } from "react";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { TilToppenKnapp } from "mulighetsrommet-frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { FilterSkeleton } from "mulighetsrommet-frontend-common";
import { Filtermeny } from "@/components/filtrering/Filtermeny";

interface Props {
  preview?: boolean;
}

export const NavArbeidsmarkedstiltakOversikt = ({ preview = false }: Props) => {
  const { data: tiltaksgjennomforinger = [], isLoading } = useNavTiltaksgjennomforinger({
    preview,
  });
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const filter = useArbeidsmarkedstiltakFilterValue();
  const { filterHasChanged, resetFilterToDefaults } =
    useResetArbeidsmarkedstiltakFilterUtenBrukerIKontekst();
  const [tagsHeight, setTagsHeight] = useState(0);

  return (
    <>
      <FilterAndTableLayout
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        buttons={null}
        filter={
          <Suspense fallback={<FilterSkeleton />}>
            <Filtermeny />
          </Suspense>
        }
        tags={<NavFiltertags filterOpen={filterOpen} setTagsHeight={setTagsHeight} />}
        nullstillFilterButton={
          filterHasChanged && <NullstillFilterKnapp onClick={resetFilterToDefaults} />
        }
        table={
          <div>
            {isLoading ? (
              <TiltakLoader />
            ) : (
              <Tiltaksgjennomforingsoversikt
                tiltaksgjennomforinger={tiltaksgjennomforinger}
                filterOpen={filterOpen}
                feilmelding={
                  !isFilterReady(filter) ? (
                    <Feilmelding
                      data-testid="filter-mangler-verdier-feilmelding"
                      header="Du må filtrere på en innsatsgruppe og minst én NAV-enhet for å se tiltaksgjennomføringer"
                      ikonvariant="info"
                    />
                  ) : tiltaksgjennomforinger.length === 0 ? (
                    <Feilmelding
                      header="Ingen tiltaksgjennomføringer funnet"
                      beskrivelse="Prøv å justere søket eller filteret for å finne det du leter etter"
                      ikonvariant="warning"
                    />
                  ) : null
                }
                tagsHeight={tagsHeight}
              />
            )}
          </div>
        }
      />
      <TilToppenKnapp />
    </>
  );
};
