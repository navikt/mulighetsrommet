import { useFeatureToggle } from "@/api/feature-toggles";
import { useVeilederTiltaksgjennomforinger } from "@/api/queries/useTiltaksgjennomforinger";
import { ModiaFiltertags } from "@/apps/modia/filtrering/ModiaFiltertags";
import { useHentAlleTiltakDeltMedBruker } from "@/apps/modia/hooks/useHentAlleTiltakDeltMedBruker";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";
import { FiltrertFeilInnsatsgruppeVarsel } from "@/apps/modia/varsler/FiltrertFeilInnsatsgruppeVarsel";
import { PortenLink } from "@/components/PortenLink";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { OversiktenJoyride } from "@/components/joyride/OversiktenJoyride";
import { Tiltaksgjennomforingsoversikt } from "@/components/oversikt/Tiltaksgjennomforingsoversikt";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { useResetArbeidsmarkedstiltakFilterMedBrukerIKontekst } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { Alert } from "@navikt/ds-react";
import { ApiError, Toggles } from "mulighetsrommet-api-client";
import { ListSkeleton, useTitle } from "mulighetsrommet-frontend-common";
import { TilToppenKnapp } from "mulighetsrommet-frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useEffect, useState } from "react";
import { ModiaOversiktBrukerVarsler } from "../varsler/ModiaOversiktBrukerVarsler";
import { FilterAndTableLayout } from "mulighetsrommet-frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { HistorikkButton } from "../historikk/HistorikkButton";
import { Filtermeny } from "@/components/filtrering/Filtermeny";

export function ModiaArbeidsmarkedstiltakOversikt() {
  useTitle("Arbeidsmarkedstiltak - Oversikt");
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const { data: brukerdata } = useHentBrukerdata();
  const { alleTiltakDeltMedBruker } = useHentAlleTiltakDeltMedBruker();

  const { filter, filterHasChanged, resetFilterToDefaults } =
    useResetArbeidsmarkedstiltakFilterMedBrukerIKontekst();

  const landingssideFeature = useFeatureToggle(Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE);
  const landingssideEnabled = landingssideFeature.isSuccess && landingssideFeature.data;
  const [isHistorikkModalOpen, setIsHistorikkModalOpen] = useState(false);
  const [tagsHeight, setTagsHeight] = useState(0);

  const {
    data: tiltaksgjennomforinger = [],
    isLoading,
    isError,
    error,
  } = useVeilederTiltaksgjennomforinger();

  useEffect(() => {
    setIsHistorikkModalOpen(isHistorikkModalOpen);
  }, [isHistorikkModalOpen]);

  if (!brukerdata) {
    return null;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Det har dessverre skjedd en feil. Om feilen gjentar seg, ta kontakt i <PortenLink />.
        {error instanceof ApiError ? (
          <pre>
            {JSON.stringify(
              { message: error.message, status: error.status, url: error.url },
              null,
              2,
            )}
          </pre>
        ) : null}
      </Alert>
    );
  }

  return (
    <>
      {landingssideEnabled ? (
        <Tilbakeknapp
          tekst="Gå til oversikt over brukerens tiltak"
          tilbakelenke="/arbeidsmarkedstiltak"
        />
      ) : null}
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
          <>
            <Tiltaksgjennomforingsoversikt
              tiltaksgjennomforinger={tiltaksgjennomforinger}
              deltMedBruker={alleTiltakDeltMedBruker ?? undefined}
              filterOpen={filterOpen}
              varsler={
                <>
                  <ModiaOversiktBrukerVarsler brukerdata={brukerdata} />
                  <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
                </>
              }
              feilmelding={
                tiltaksgjennomforinger.length === 0 ? (
                  isLoading ? (
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
          </>
        }
      />
      <TilToppenKnapp />
    </>
  );
}
