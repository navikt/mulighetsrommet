import { useModiaArbeidsmarkedstiltak } from "@/api/queries/useArbeidsmarkedstiltak";
import { ModiaFilterTags } from "@/apps/modia/filtrering/ModiaFilterTags";
import { FiltrertFeilInnsatsgruppeVarsel } from "@/apps/modia/varsler/FiltrertFeilInnsatsgruppeVarsel";
import { Melding } from "@/components/melding/Melding";
import { FilterMenu } from "@/components/filtrering/FilterMenu";
import { ArbeidsmarkedstiltakList } from "@/components/oversikt/ArbeidsmarkedstiltakList";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import {
  isFilterReady,
  useArbeidsmarkedstiltakFilterMedBrukerIKontekst,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { ListSkeleton, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useState } from "react";
import { ModiaOversiktBrukerVarsler } from "../varsler/ModiaOversiktBrukerVarsler";
import { Box } from "@navikt/ds-react";
import { useDelMedBrukerHistorikk } from "../hooks/useDelMedBrukerHistorikk";

export function ModiaArbeidsmarkedstiltakOversikt() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const { data: alleDelMedBruker } = useDelMedBrukerHistorikk();

  const { filter, filterHasChanged, resetFilterToDefaults } =
    useArbeidsmarkedstiltakFilterMedBrukerIKontekst();

  const { data: tiltak = [], isPending } = useModiaArbeidsmarkedstiltak();

  return (
    <>
      <title>Arbeidsmarkedstiltak - Oversikt</title>
      <Tilbakeknapp
        tekst="Gå til oversikt over brukerens tiltak"
        tilbakelenke="/arbeidsmarkedstiltak"
      />
      <FilterAndTableLayout
        hasChanged={filterHasChanged}
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        nullstillFilterButton={<NullstillFilterKnapp onClick={resetFilterToDefaults} />}
        lagreFilterButton={null}
        buttons={null}
        filter={<FilterMenu />}
        tags={<ModiaFilterTags filterOpen={filterOpen} setTagsHeight={setTagsHeight} />}
        table={
          <ArbeidsmarkedstiltakList
            tiltak={tiltak}
            alleDelMedBruker={alleDelMedBruker ?? undefined}
            filterOpen={filterOpen}
            varsler={
              <>
                <ModiaOversiktBrukerVarsler />
                <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
              </>
            }
            feilmelding={
              !isFilterReady(filter) ? (
                <Box paddingInline="space-6 space-0">
                  <Melding
                    data-testid="filter-mangler-verdier-feilmelding"
                    header="Filter mangler"
                    variant="info"
                  >
                    Du må filtrere på en innsatsgruppe og minst én Nav-enhet for å se tiltak
                  </Melding>
                </Box>
              ) : tiltak.length === 0 ? (
                isPending ? (
                  <ListSkeleton />
                ) : (
                  <Box paddingInline="space-6 space-0">
                    <Melding header="Ingen tiltak funnet" variant="warning">
                      Prøv å justere søket eller filteret for å finne det du leter etter
                    </Melding>
                  </Box>
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
