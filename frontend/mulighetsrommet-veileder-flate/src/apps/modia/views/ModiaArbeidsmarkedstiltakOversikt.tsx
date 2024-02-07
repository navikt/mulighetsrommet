import { useEffect, useState } from "react";
import { Alert, Button } from "@navikt/ds-react";
import { ApiError, Toggles } from "mulighetsrommet-api-client";
import { useTitle } from "mulighetsrommet-frontend-common";
import { PORTEN_MODIA } from "mulighetsrommet-frontend-common/constants";
import { Lenke } from "mulighetsrommet-frontend-common/components/Lenke";
import { TiltakLoader } from "@/components/TiltakLoader";
import { BrukersOppfolgingsenhetVarsel } from "@/apps/modia/varsler/BrukersOppfolgingsenhetVarsel";
import { Feilmelding, ForsokPaNyttLink } from "@/components/feilmelding/Feilmelding";
import { FilterAndTableLayout } from "@/components/filtrering/FilterAndTableLayout";
import { ModiaFilterTags } from "@/apps/modia/filtrering/ModiaFilterTags";
import { HistorikkButton } from "@/apps/modia/historikk/HistorikkButton";
import { BrukerHarIkke14aVedtakVarsel } from "@/apps/modia/varsler/BrukerHarIkke14aVedtakVarsel";
import { FiltrertFeilInnsatsgruppeVarsel } from "@/apps/modia/varsler/FiltrertFeilInnsatsgruppeVarsel";
import { OversiktenJoyride } from "@/components/joyride/OversiktenJoyride";
import { Tiltaksgjennomforingsoversikt } from "@/components/oversikt/Tiltaksgjennomforingsoversikt";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { useFeatureToggle } from "@/core/api/feature-toggles";
import { useHentAlleTiltakDeltMedBruker } from "@/apps/modia/hooks/useHentAlleTiltakDeltMedBruker";
import { useHentBrukerdata } from "@/apps/modia/hooks/useHentBrukerdata";
import { useVeilederTiltaksgjennomforinger } from "@/core/api/queries/useTiltaksgjennomforinger";
import { useResetArbeidsmarkedstiltakFilterMedBrukerIKontekst } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { ManglerInnsatsOgServicegruppeVarsel } from "@/apps/modia/varsler/ManglerInnsatsOgServiceGruppeVarsel";
import { FilterMenyMedSkeletonLoader } from "@/components/filtrering/FilterMenyMedSkeletonLoader";

export const ModiaArbeidsmarkedstiltakOversikt = () => {
  useTitle("Arbeidsmarkedstiltak - Oversikt");

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
    if (error instanceof ApiError) {
      return (
        <Alert variant="error">
          Det har dessverre skjedd en feil. Om feilen gjentar seg, ta kontakt i{" "}
          {
            <Lenke to={PORTEN_MODIA} target={"_blank"}>
              Porten
            </Lenke>
          }
          <pre>
            {JSON.stringify(
              { message: error.message, status: error.status, url: error.url },
              null,
              2,
            )}
          </pre>
        </Alert>
      );
    } else {
      return (
        <Alert variant="error">
          Det har dessverre skjedd en feil. Om feilen gjentar seg, ta kontakt i{" "}
          {
            <Lenke to={PORTEN_MODIA} target={"_blank"}>
              Porten
            </Lenke>
          }
          .
        </Alert>
      );
    }
  }

  if (brukerdata.enheter.length === 0) {
    return (
      <Feilmelding
        header="Kunne ikke hente brukers geografiske enhet"
        beskrivelse={
          <>
            Brukers geografiske enhet kunne ikke hentes. Kontroller at brukeren er under oppfølging
            og finnes i Arena, og <ForsokPaNyttLink />
          </>
        }
        ikonvariant="error"
      />
    );
  }

  if (!brukerdata.innsatsgruppe && !brukerdata.servicegruppe) {
    return (
      <Feilmelding
        header="Kunne ikke hente brukers innsatsgruppe eller servicegruppe"
        beskrivelse={
          <>
            Vi kan ikke hente brukerens innsatsgruppe eller servicegruppe. Kontroller at brukeren er
            under oppfølging og finnes i Arena, og <br /> <ForsokPaNyttLink />
          </>
        }
        ikonvariant="error"
      />
    );
  }

  return (
    <>
      {landingssideEnabled ? <Tilbakeknapp tilbakelenke="/arbeidsmarkedstiltak" /> : null}
      <FilterAndTableLayout
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
        tags={<ModiaFilterTags />}
        table={
          <div style={{ marginTop: "1rem" }}>
            <BrukersOppfolgingsenhetVarsel brukerdata={brukerdata} />
            <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
            <BrukerHarIkke14aVedtakVarsel brukerdata={brukerdata} />
            <ManglerInnsatsOgServicegruppeVarsel brukerdata={brukerdata} />
            {isLoading ? (
              <TiltakLoader />
            ) : tiltaksgjennomforinger.length === 0 ? (
              <TilbakestillFilterFeil resetFilter={resetFilterToDefaults} />
            ) : (
              <Tiltaksgjennomforingsoversikt
                tiltaksgjennomforinger={tiltaksgjennomforinger}
                deltMedBruker={alleTiltakDeltMedBruker}
              />
            )}
          </div>
        }
      />
    </>
  );
};

function TilbakestillFilterFeil({ resetFilter }: { resetFilter(): void }) {
  return (
    <Feilmelding
      header="Ingen tiltaksgjennomføringer funnet"
      beskrivelse="Prøv å justere søket eller filteret for å finne det du leter etter"
      ikonvariant="warning"
    >
      <Button variant="tertiary" onClick={resetFilter}>
        Tilbakestill filter
      </Button>
    </Feilmelding>
  );
}
