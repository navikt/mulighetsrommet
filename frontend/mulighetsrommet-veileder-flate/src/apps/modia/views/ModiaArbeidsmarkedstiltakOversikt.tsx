import { useEffect, useState } from "react";
import { Alert } from "@navikt/ds-react";
import { ApiError, Toggles } from "mulighetsrommet-api-client";
import { useTitle } from "mulighetsrommet-frontend-common";
import { TiltakLoader } from "@/components/TiltakLoader";
import { FilterAndTableLayout } from "@/components/filtrering/FilterAndTableLayout";
import { HistorikkButton } from "@/apps/modia/historikk/HistorikkButton";
import { OversiktenJoyride } from "@/components/joyride/OversiktenJoyride";
import { Tiltaksgjennomforingsoversikt } from "@/components/oversikt/Tiltaksgjennomforingsoversikt";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { useFeatureToggle } from "@/core/api/feature-toggles";
import { useHentAlleTiltakDeltMedBruker } from "@/apps/modia/hooks/useHentAlleTiltakDeltMedBruker";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";
import { useVeilederTiltaksgjennomforinger } from "@/core/api/queries/useTiltaksgjennomforinger";
import { FilterMenyMedSkeletonLoader } from "@/components/filtrering/FilterMenyMedSkeletonLoader";
import { PortenLink } from "@/components/PortenLink";
import { useResetArbeidsmarkedstiltakFilterMedBrukerIKontekst } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { BrukerHarIkke14aVedtakVarsel } from "@/apps/modia/varsler/BrukerHarIkke14aVedtakVarsel";
import { BrukersOppfolgingsenhetVarsel } from "@/apps/modia/varsler/BrukersOppfolgingsenhetVarsel";
import { FiltrertFeilInnsatsgruppeVarsel } from "@/apps/modia/varsler/FiltrertFeilInnsatsgruppeVarsel";
import { ModiaFiltertags } from "@/apps/modia/filtrering/ModiaFiltertags";
import { Feilmelding } from "@/components/feilmelding/Feilmelding";
import { TilToppenKnapp } from "../../../../../frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { NullstillFilterKnapp } from "../../../../../frontend-common/components/filter/nullstillFilterKnapp/NullstillFilterKnapp";

export const ModiaArbeidsmarkedstiltakOversikt = () => {
  useTitle("Arbeidsmarkedstiltak - Oversikt");
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const { data: brukerdata } = useHentBrukerdata();
  const { alleTiltakDeltMedBruker } = useHentAlleTiltakDeltMedBruker();

  const { filter, filterHasChanged, resetFilterToDefaults } =
    useResetArbeidsmarkedstiltakFilterMedBrukerIKontekst();

  const landingssideFeature = useFeatureToggle(Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE);
  const landingssideEnabled = landingssideFeature.isSuccess && landingssideFeature.data;

  const [isHistorikkModalOpen, setIsHistorikkModalOpen] = useState(false);

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
      {landingssideEnabled ? <Tilbakeknapp tilbakelenke="/arbeidsmarkedstiltak" /> : null}
      <FilterAndTableLayout
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        resetButton={filterHasChanged && <NullstillFilterKnapp onClick={resetFilterToDefaults} />}
        buttons={
          <>
            <OversiktenJoyride />
            <HistorikkButton
              setHistorikkModalOpen={setIsHistorikkModalOpen}
              isHistorikkModalOpen={isHistorikkModalOpen}
            />
          </>
        }
        filter={<FilterMenyMedSkeletonLoader />}
        table={
          <div>
            {isLoading ? (
              <TiltakLoader />
            ) : (
              <Tiltaksgjennomforingsoversikt
                tiltaksgjennomforinger={tiltaksgjennomforinger}
                deltMedBruker={alleTiltakDeltMedBruker}
                filterOpen={filterOpen}
                varsler={
                  <>
                    <BrukerHarIkke14aVedtakVarsel brukerdata={brukerdata} />
                    <BrukersOppfolgingsenhetVarsel brukerdata={brukerdata} />
                    <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
                  </>
                }
                tags={<ModiaFiltertags filterOpen={filterOpen} />}
                feilmelding={
                  tiltaksgjennomforinger.length === 0 ? (
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
      <TilToppenKnapp />
    </>
  );
};
