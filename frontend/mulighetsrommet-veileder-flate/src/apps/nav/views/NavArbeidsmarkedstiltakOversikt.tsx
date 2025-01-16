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
import { LagredeFilterOversikt, LagreFilterButton, ListSkeleton } from "@mr/frontend-common";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { HStack } from "@navikt/ds-react";
import { Suspense, useState } from "react";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { useSlettFilter } from "@/api/lagret-filter/useSlettFilter";
import { useLagreFilter } from "@/api/lagret-filter/useLagreFilter";

interface Props {
  preview?: boolean;
}

export function NavArbeidsmarkedstiltakOversikt({ preview = false }: Props) {
  const { data: tiltak = [], isPending } = useNavArbeidsmarkedstiltak({
    preview,
  });
  const [filterOpen, setFilterOpen] = useState(true);
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  const filterValue = useArbeidsmarkedstiltakFilterValue();
  const { filterHasChanged, resetFilterToDefaults } =
    useResetArbeidsmarkedstiltakFilterUtenBrukerIKontekst();
  const [tagsHeight, setTagsHeight] = useState(0);
  const { data: lagredeFilter = [] } = useLagredeFilter(LagretDokumenttype.GJENNOMFORING_MODIA);
  const deleteFilterMutation = useSlettFilter(LagretDokumenttype.GJENNOMFORING_MODIA);
  const lagreFilterMutation = useLagreFilter(LagretDokumenttype.GJENNOMFORING_MODIA);

  return (
    <>
      <FilterAndTableLayout
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        buttons={null}
        filter={
          <Suspense fallback={<div>loading...</div>}>
            <Filtermeny />
          </Suspense>
        }
        tags={<NavFiltertags filterOpen={filterOpen} setTagsHeight={setTagsHeight} />}
        nullstillFilterButton={
          filterHasChanged && (
            <HStack gap="2">
              <NullstillFilterKnapp onClick={resetFilterToDefaults} />
              <LagreFilterButton
                onLagre={(r) => {
                  lagreFilterMutation.mutate(r);
                  lagreFilterMutation.reset();
                }}
                dokumenttype={LagretDokumenttype.GJENNOMFORING_MODIA}
                filter={filterValue}
              />
            </HStack>
          )
        }
        lagredeFilter={
          <LagredeFilterOversikt
            onDelete={(id: string) => deleteFilterMutation.mutate(id)}
            lagredeFilter={lagredeFilter}
            filter={filter}
            setFilter={setFilter}
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
              !isFilterReady(filterValue) ? (
                <Feilmelding
                  data-testid="filter-mangler-verdier-feilmelding"
                  header="Du må filtrere på en innsatsgruppe og minst én Nav-enhet for å se tiltaksgjennomføringer"
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
