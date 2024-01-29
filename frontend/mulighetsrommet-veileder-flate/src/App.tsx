import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import "@navikt/ds-css";
import { Navigate, Route, Routes } from "react-router-dom";
import { useHentVeilederdata } from "./core/api/queries/useHentVeilederdata";
import { useInitializeArbeidsmarkedstiltakFilterForBruker } from "./hooks/useInitializeArbeidsmarkedstiltakFilterForBruker";
import { useInitializeAppContext } from "./hooks/useInitializeAppContext";
import { initAmplitude } from "./logging/amplitude";
import { useFeatureToggle } from "./core/api/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";
import { Landingsside } from "./views/landingsside/Landingsside";
import { ModiaArbeidsmarkedstiltakDetaljer } from "./views/modia-arbeidsmarkedstiltak/ModiaArbeidsmarkedstiltakDetaljer";
import { DeltakerRegistrering } from "./microfrontends/team_komet/DeltakerRegistrering";
import { ModiaArbeidsmarkedstiltakOversikt } from "./views/modia-arbeidsmarkedstiltak/ModiaArbeidsmarkedstiltakOversikt";
import { ArbeidsmarkedstiltakHeader } from "./components/ArbeidsmarkedstiltakHeader";
import { NavArbeidsmarkedstiltakOversikt } from "./views/nav-arbeidsmarkedstiltak/NavArbeidsmarkedstiltakOversikt";
import { NavArbeidsmarkedstiltakDetaljer } from "./views/nav-arbeidsmarkedstiltak/NavArbeidsmarkedstiltakDetaljer";
import { AppContainerOversiktView } from "./components/appContainerOversiktView/AppContainerOversiktView";
import { DemoImageHeader } from "./components/DemoImageHeader";
import { PreviewArbeidsmarkedstiltakOversikt } from "./views/preview/PreviewArbeidsmarkedstiltakOversikt";
import { PreviewArbeidsmarkedstiltakDetaljer } from "./views/preview/PreviewArbeidsmarkedstiltakDetaljer";

if (import.meta.env.PROD && import.meta.env.VITE_FARO_URL) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL,
    instrumentations: [...getWebInstrumentations({ captureConsole: true })],
    app: {
      name: "mulighetsrommet-veileder-flate",
    },
  });
  initAmplitude();
}

export function ModiaArbeidsmarkedstiltak() {
  useHentVeilederdata(); // Pre-fetch veilederdata så slipper vi å vente på data når vi trenger det i appen senere

  const { fnr, enhet } = useInitializeAppContext();

  useInitializeArbeidsmarkedstiltakFilterForBruker();

  const enableLandingssideFeature = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE,
  );
  const visDeltakerregistreringFeature = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING,
  );
  const enableLandingsside = enableLandingssideFeature.isSuccess && enableLandingssideFeature.data;
  const visDeltakerregistrering =
    visDeltakerregistreringFeature.isSuccess && visDeltakerregistreringFeature.data;

  if (enableLandingssideFeature.isLoading) {
    return null;
  }

  return (
    <AppContainerOversiktView header={<DemoImageHeader />}>
      <Routes>
        {enableLandingsside ? <Route path="" element={<Landingsside />} /> : null}
        <Route path="oversikt" element={<ModiaArbeidsmarkedstiltakOversikt />} />
        <Route path="tiltak/:id/*" element={<ModiaArbeidsmarkedstiltakDetaljer />} />
        {visDeltakerregistrering ? (
          <Route
            path="tiltak/:id/deltaker"
            element={<DeltakerRegistrering fnr={fnr} enhetId={enhet} />}
          />
        ) : null}
        <Route
          path="*"
          element={
            <Navigate replace to={enableLandingsside ? "/arbeidsmarkedstiltak" : "./oversikt"} />
          }
        />
      </Routes>
    </AppContainerOversiktView>
  );
}

export function NavArbeidsmarkedstiltak() {
  return (
    <AppContainerOversiktView header={<ArbeidsmarkedstiltakHeader />}>
      <Routes>
        <Route path="oversikt" element={<NavArbeidsmarkedstiltakOversikt />} />
        <Route path="tiltak/:id/*" element={<NavArbeidsmarkedstiltakDetaljer />} />
        <Route path="*" element={<Navigate replace to="./oversikt" />} />
      </Routes>
    </AppContainerOversiktView>
  );
}

export function PreviewArbeidsmarkedstiltak() {
  return (
    <AppContainerOversiktView header={<ArbeidsmarkedstiltakHeader />}>
      <Routes>
        <Route path="oversikt" element={<PreviewArbeidsmarkedstiltakOversikt />} />
        <Route path="tiltak/:id/*" element={<PreviewArbeidsmarkedstiltakDetaljer />} />
        <Route path="*" element={<Navigate replace to="./oversikt" />} />
      </Routes>
    </AppContainerOversiktView>
  );
}
