import { TiltakLoader } from "@/components/TiltakLoader";
import { FilterAndTableLayout } from "@/components/filtrering/FilterAndTableLayout";
import { Tiltaksgjennomforingsoversikt } from "@/components/oversikt/Tiltaksgjennomforingsoversikt";
import { useNavTiltaksgjennomforinger } from "@/core/api/queries/useTiltaksgjennomforinger";
import { FilterMenyMedSkeletonLoader } from "@/components/filtrering/FilterMenyMedSkeletonLoader";
import { Button } from "@navikt/ds-react";
import {
  isFilterReady,
  useArbeidsmarkedstiltakFilterValue,
  useResetArbeidsmarkedstiltakFilterUtenBrukerIKontekst,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { NavFilterTags } from "@/apps/nav/filtrering/NavFilterTags";
import { useState } from "react";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { TilToppenKnapp } from "../../../../../frontend-common/components/tilToppenKnapp/TilToppenKnapp";

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

  return (
    <>
      <FilterAndTableLayout
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        buttons={null}
        filter={<FilterMenyMedSkeletonLoader />}
        resetButton={
          filterHasChanged && (
            <Button
              size="small"
              variant="tertiary"
              onClick={resetFilterToDefaults}
              data-testid="knapp_nullstill-filter"
            >
              Nullstill filter
            </Button>
          )
        }
        table={
          <div>
            {isLoading ? (
              <TiltakLoader />
            ) : (
              <Tiltaksgjennomforingsoversikt
                tiltaksgjennomforinger={tiltaksgjennomforinger}
                tags={<NavFilterTags />}
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
              />
            )}
          </div>
        }
      />
      <TilToppenKnapp />;
    </>
  );
};
