import { useModiaArbeidsmarkedstiltak } from "@/api/queries/useArbeidsmarkedstiltak";
import { ModiaFilterTags } from "@/apps/modia/filtrering/ModiaFilterTags";
import { useAlleTiltakDeltMedBruker } from "@/apps/modia/hooks/useAlleTiltakDeltMedBruker";
import { FiltrertFeilInnsatsgruppeVarsel } from "@/apps/modia/varsler/FiltrertFeilInnsatsgruppeVarsel";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { FilterMenu } from "@/components/filtrering/FilterMenu";
import { OversiktenJoyride } from "@/components/joyride/OversiktenJoyride";
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
import { BodyShort } from "@navikt/ds-react";
import { useState } from "react";
import { ModiaOversiktBrukerVarsler } from "../varsler/ModiaOversiktBrukerVarsler";

export function ModiaArbeidsmarkedstiltakOversikt() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const { data: alleTiltakDeltMedBruker } = useAlleTiltakDeltMedBruker();

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
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        nullstillFilterButton={
          filterHasChanged && <NullstillFilterKnapp onClick={resetFilterToDefaults} />
        }
        buttons={
          <>
            <OversiktenJoyride />
          </>
        }
        filter={<FilterMenu />}
        tags={<ModiaFilterTags filterOpen={filterOpen} setTagsHeight={setTagsHeight} />}
        table={
          <ArbeidsmarkedstiltakList
            tiltak={tiltak}
            deltMedBruker={alleTiltakDeltMedBruker ?? undefined}
            filterOpen={filterOpen}
            varsler={
              <>
                <ModiaOversiktBrukerVarsler />
                <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
              </>
            }
            feilmelding={
              !isFilterReady(filter) ? (
                <Feilmelding
                  data-testid="filter-mangler-verdier-feilmelding"
                  header="Du må filtrere på en innsatsgruppe og minst én Nav-enhet for å se tiltak"
                  ikonvariant="info"
                />
              ) : tiltak.length === 0 ? (
                isPending ? (
                  <ListSkeleton />
                ) : (
                  <Feilmelding header="Ingen tiltak funnet" ikonvariant="warning">
                    <BodyShort size="small">
                      Prøv å justere søket eller filteret for å finne det du leter etter
                    </BodyShort>
                  </Feilmelding>
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
