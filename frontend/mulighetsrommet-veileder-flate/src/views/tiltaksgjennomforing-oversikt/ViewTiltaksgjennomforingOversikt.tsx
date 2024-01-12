import { Alert, Button, Loader } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { RESET } from "jotai/utils";
import { ApiError, Innsatsgruppe, Toggles } from "mulighetsrommet-api-client";
import { useTitle } from "mulighetsrommet-frontend-common";
import { PORTEN } from "mulighetsrommet-frontend-common/constants";
import { useEffect, useState } from "react";
import { BrukersOppfolgingsenhetVarsel } from "../../components/brukersEnheter/BrukersOppfolgingsenhetVarsel";
import { Feilmelding, ForsokPaNyttLink } from "../../components/feilmelding/Feilmelding";
import Filtermeny from "../../components/filtrering/Filtermeny";
import { Filtertags } from "../../components/filtrering/Filtertags";
import { BrukerHarIkke14aVedtakVarsel } from "../../components/ikkeKvalifisertVarsel/BrukerHarIkke14aVedtakVarsel";
import { FiltrertFeilInnsatsgruppeVarsel } from "../../components/ikkeKvalifisertVarsel/FiltrertFeilInnsatsgruppeVarsel";
import Lenke from "../../components/lenke/Lenke";
import Tiltaksgjennomforingsoversikt from "../../components/oversikt/Tiltaksgjennomforingsoversikt";
import Tilbakeknapp from "../../components/tilbakeknapp/Tilbakeknapp";
import { useFeatureToggle } from "../../core/api/feature-toggles";
import { useHentBrukerdata } from "../../core/api/queries/useHentBrukerdata";
import useTiltaksgjennomforinger from "../../core/api/queries/useTiltaksgjennomforinger";
import { tiltaksgjennomforingsfilter } from "../../core/atoms/atoms";
import { usePrepopulerFilter } from "../../hooks/usePrepopulerFilter";
import { routes } from "../../routes";
import styles from "./ViewTiltaksgjennomforingOversikt.module.scss";
import { useLogEvent } from "../../logging/amplitude";
import { FilterAndTableLayout } from "../../components/filtrering/FilterAndTableLayout";
import { OversiktenJoyride } from "../../components/joyride/OversiktenJoyride";
import { HistorikkButton } from "../../components/historikk/HistorikkButton";
import { useInnsatsgrupper } from "../../core/api/queries/useInnsatsgrupper";
import { useHentAlleTiltakDeltMedBruker } from "../../core/api/queries/useHentAlleTiltakDeltMedBruker";

const ViewTiltaksgjennomforingOversikt = () => {
  useTitle("Arbeidsmarkedstiltak - Oversikt");
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const { data: brukerdata, isFetched: brukerdataIsFetched } = useHentBrukerdata();
  const landingssideFeature = useFeatureToggle(Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE);
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  const { isLoading: innsatsgrupperIsLoading } = useInnsatsgrupper();
  const { logEvent } = useLogEvent();

  const landingssideEnabled = landingssideFeature.isSuccess && landingssideFeature.data;
  const [isHistorikkModalOpen, setIsHistorikkModalOpen] = useState(false);

  const { alleTiltakDeltMedBruker } = useHentAlleTiltakDeltMedBruker();

  const {
    data: tiltaksgjennomforinger = [],
    isLoading,
    isError,
    error,
    isFetching,
  } = useTiltaksgjennomforinger();

  useEffect(() => {
    setIsHistorikkModalOpen(isHistorikkModalOpen);
  }, [isHistorikkModalOpen]);

  useEffect(() => {
    if (brukerdataIsFetched) {
      logEvent({ name: "arbeidsmarkedstiltak.unike-brukere" });
    }
  }, []);

  const brukersInnsatsgruppeErIkkeValgt = (innsatsgruppe?: Innsatsgruppe) => {
    return innsatsgruppe !== brukerdata?.innsatsgruppe;
  };

  const visNullstillButton =
    !innsatsgrupperIsLoading &&
    (brukersInnsatsgruppeErIkkeValgt(filter.innsatsgruppe?.nokkel) ||
      filter.search !== "" ||
      filter.tiltakstyper.length > 0);

  if (!brukerdata) return null;

  if (isError) {
    if (error instanceof ApiError) {
      return (
        <Alert variant="error">
          Det har dessverre skjedd en feil. Om feilen gjentar seg, ta kontakt i{" "}
          {
            <Lenke to={PORTEN} target={"_blank"}>
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
            <Lenke to={PORTEN} target={"_blank"}>
              Porten
            </Lenke>
          }
          .
        </Alert>
      );
    }
  }

  if (!brukerdata.geografiskEnhet) {
    return (
      <Feilmelding
        header="Kunne ikke hente brukers geografiske enhet"
        beskrivelse={
          <>
            Brukers geografiske enhet kunne ikke hentes. Kontroller at brukeren er under oppfølging
            og finnes i Arena, og {ForsokPaNyttLink()}
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
            under oppfølging og finnes i Arena, og <br /> {ForsokPaNyttLink()}
          </>
        }
        ikonvariant="error"
      />
    );
  }

  return (
    <>
      {landingssideEnabled ? <Tilbakeknapp tilbakelenke={`/${routes.base}`} /> : null}
      <FilterAndTableLayout
        resetButton={
          visNullstillButton && (
            <Button
              size="small"
              variant="tertiary"
              onClick={() => {
                setFilter(RESET);
                forcePrepopulerFilter(true);
              }}
              data-testid="knapp_tilbakestill-filter"
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
        filter={<Filtermeny />}
        tags={<Filtertags />}
        table={
          <div style={{ marginTop: "1rem" }}>
            <BrukersOppfolgingsenhetVarsel brukerdata={brukerdata} />
            <FiltrertFeilInnsatsgruppeVarsel filter={filter} />
            <BrukerHarIkke14aVedtakVarsel brukerdata={brukerdata} />
            {isLoading ? (
              <div className={styles.filter_loader}>
                <Loader />
              </div>
            ) : tiltaksgjennomforinger.length === 0 ? (
              <TilbakestillFilterFeil />
            ) : (
              <Tiltaksgjennomforingsoversikt
                tiltaksgjennomforinger={tiltaksgjennomforinger}
                isFetching={isFetching}
                deltMedBruker={alleTiltakDeltMedBruker}
              />
            )}
          </div>
        }
      />
    </>
  );
};

export function TilbakestillFilterFeil() {
  const [, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const { forcePrepopulerFilter } = usePrepopulerFilter();

  return (
    <Feilmelding
      header="Ingen tiltaksgjennomføringer funnet"
      beskrivelse="Prøv å justere søket eller filteret for å finne det du leter etter"
      ikonvariant="warning"
    >
      <>
        <Button
          variant="tertiary"
          onClick={() => {
            setFilter(RESET);
            forcePrepopulerFilter(true);
          }}
        >
          Tilbakestill filter
        </Button>
      </>
    </Feilmelding>
  );
}

export default ViewTiltaksgjennomforingOversikt;
