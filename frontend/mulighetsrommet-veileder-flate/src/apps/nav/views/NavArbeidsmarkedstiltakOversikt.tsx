import { NavFiltertags } from "@/apps/nav/filtrering/NavFiltertags";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { Filtermeny } from "@/components/filtrering/Filtermeny";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { ArbeidsmarkedstiltakList } from "@/components/oversikt/ArbeidsmarkedstiltakList";
import { useNavArbeidsmarkedstiltak } from "@/api/queries/useArbeidsmarkedstiltak";
import {
  ArbeidsmarkedstiltakFilterSchema,
  isFilterReady,
  useArbeidsmarkedstiltakFilter,
  useArbeidsmarkedstiltakFilterValue,
  useResetArbeidsmarkedstiltakFilterUtenBrukerIKontekst,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { LagretDokumenttype } from "@mr/api-client";
import { LagredeFilterOversikt, LagreFilterContainer, ListSkeleton } from "@mr/frontend-common";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { HStack } from "@navikt/ds-react";
import { useState } from "react";

interface Props {
  preview?: boolean;
}

export function NavArbeidsmarkedstiltakOversikt({ preview = false }: Props) {
  const { data: tiltak = [], isPending } = useNavArbeidsmarkedstiltak({
    preview,
  });
  const [filterOpen, setFilterOpen] = useState(true);
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
          <ArbeidsmarkedstiltakList
            tiltak={tiltak}
            filterOpen={filterOpen}
            feilmelding={
              !isFilterReady(filter) ? (
                <Feilmelding
                  data-testid="filter-mangler-verdier-feilmelding"
                  header="Du må filtrere på en innsatsgruppe og minst én NAV-enhet for å se tiltaksgjennomføringer"
                  ikonvariant="info"
                />
              ) : tiltak.length === 0 ? (
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
