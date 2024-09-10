import { useModiaArbeidsmarkedstiltak } from "@/api/queries/useArbeidsmarkedstiltak";
import { ModiaFiltertags } from "@/apps/modia/filtrering/ModiaFiltertags";
import { useHentAlleTiltakDeltMedBruker } from "@/apps/modia/hooks/useHentAlleTiltakDeltMedBruker";
import { FiltrertFeilInnsatsgruppeVarsel } from "@/apps/modia/varsler/FiltrertFeilInnsatsgruppeVarsel";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { Filtermeny } from "@/components/filtrering/Filtermeny";
import { OversiktenJoyride } from "@/components/joyride/OversiktenJoyride";
import { ArbeidsmarkedstiltakList } from "@/components/oversikt/ArbeidsmarkedstiltakList";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import {
  isFilterReady,
  useResetArbeidsmarkedstiltakFilterMedBrukerIKontekst,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { ListSkeleton, useOpenFilterWhenThreshold, useTitle } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useEffect, useState } from "react";
import { HistorikkButton } from "../historikk/HistorikkButton";
import { ModiaOversiktBrukerVarsler } from "../varsler/ModiaOversiktBrukerVarsler";

export function ModiaArbeidsmarkedstiltakOversikt() {
  useTitle("Arbeidsmarkedstiltak - Oversikt");
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const { alleTiltakDeltMedBruker } = useHentAlleTiltakDeltMedBruker();

  const { filter, filterHasChanged, resetFilterToDefaults } =
    useResetArbeidsmarkedstiltakFilterMedBrukerIKontekst();

  const [isHistorikkModalOpen, setIsHistorikkModalOpen] = useState(false);
  const [tagsHeight, setTagsHeight] = useState(0);

  const { data: tiltak = [], isPending } = useModiaArbeidsmarkedstiltak();

  useEffect(() => {
    setIsHistorikkModalOpen(isHistorikkModalOpen);
  }, [isHistorikkModalOpen]);

  return (
    <>
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
            <HistorikkButton
              setHistorikkModalOpen={setIsHistorikkModalOpen}
              isHistorikkModalOpen={isHistorikkModalOpen}
            />
          </>
        }
        filter={<Filtermeny />}
        tags={<ModiaFiltertags filterOpen={filterOpen} setTagsHeight={setTagsHeight} />}
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
