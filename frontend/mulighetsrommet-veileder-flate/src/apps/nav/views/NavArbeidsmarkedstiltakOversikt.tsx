import { FilterAndTableLayout } from "mulighetsrommet-frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { Tiltaksgjennomforingsoversikt } from "@/components/oversikt/Tiltaksgjennomforingsoversikt";
import { useNavTiltaksgjennomforinger } from "@/api/queries/useTiltaksgjennomforinger";
import {
  ArbeidsmarkedstiltakFilterSchema,
  isFilterReady,
  useArbeidsmarkedstiltakFilter,
  useArbeidsmarkedstiltakFilterValue,
  useResetArbeidsmarkedstiltakFilterUtenBrukerIKontekst,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { NavFiltertags } from "@/apps/nav/filtrering/NavFiltertags";
import { useState } from "react";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { TilToppenKnapp } from "mulighetsrommet-frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { Filtermeny } from "@/components/filtrering/Filtermeny";
import {
  LagredeFilterOversikt,
  LagreFilterContainer,
  ListSkeleton,
} from "mulighetsrommet-frontend-common";
import { LagretDokumenttype } from "mulighetsrommet-api-client";
import { HStack } from "@navikt/ds-react";

interface Props {
  preview?: boolean;
}

export function NavArbeidsmarkedstiltakOversikt({ preview = false }: Props) {
  const { data: tiltaksgjennomforinger = [], isPending } = useNavTiltaksgjennomforinger({
    preview,
  });
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const [lagredeFilter, setLagredeFilter] = useArbeidsmarkedstiltakFilter();
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
        filter={<Filtermeny />}
        tags={<NavFiltertags filterOpen={filterOpen} setTagsHeight={setTagsHeight} />}
        nullstillFilterButton={
          filterHasChanged && (
            <HStack gap="2">
              <NullstillFilterKnapp onClick={resetFilterToDefaults} />
              <LagreFilterContainer
                dokumenttype={LagretDokumenttype.TILTAKSGJENNOMFØRING_MODIA}
                filter={filter}
              />
            </HStack>
          )
        }
        lagredeFilter={
          <LagredeFilterOversikt
            dokumenttype={LagretDokumenttype.TILTAKSGJENNOMFØRING_MODIA}
            filter={lagredeFilter}
            setFilter={setLagredeFilter}
            validateFilterStructure={(filter) => {
              return ArbeidsmarkedstiltakFilterSchema.safeParse(filter).success;
            }}
          />
        }
        table={
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
                isPending ? (
                  <ListSkeleton />
                ) : (
                  <Feilmelding
                    header="Ingen tiltaksgjennomføringer funnet"
                    beskrivelse="Prøv å justere søket eller filteret for å finne det du leter etter"
                    ikonvariant="warning"
                  />
                )
              ) : null
            }
            tagsHeight={tagsHeight}
          />
        }
      />
      <TilToppenKnapp />
    </>
  );
}
