import "@navikt/ds-css";
import "./polyfill";
import { Navigate, Route, Routes } from "react-router-dom";
import { useHentVeilederdata } from "../../core/api/queries/useHentVeilederdata";
import { useInitializeAppContext } from "../../hooks/useInitializeAppContext";
import { useInitializeArbeidsmarkedstiltakFilterForBruker } from "../../hooks/useInitializeArbeidsmarkedstiltakFilterForBruker";
import { useFeatureToggle } from "../../core/api/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";
import { AppContainer } from "../../layouts/AppContainer";
import { DemoImageHeader } from "../../components/DemoImageHeader";
import { Landingsside } from "./views/Landingsside";
import { ModiaArbeidsmarkedstiltakOversikt } from "./views/ModiaArbeidsmarkedstiltakOversikt";
import { ModiaArbeidsmarkedstiltakDetaljer } from "./views/ModiaArbeidsmarkedstiltakDetaljer";
import { DeltakerRegistrering } from "../../microfrontends/team_komet/DeltakerRegistrering";

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
    <AppContainer header={<DemoImageHeader />}>
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
    </AppContainer>
  );
}
