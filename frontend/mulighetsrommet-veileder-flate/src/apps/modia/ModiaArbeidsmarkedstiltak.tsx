import "@navikt/ds-css";
import "./polyfill";
import { Navigate, Route, Routes } from "react-router-dom";
import { useHentVeilederdata } from "@/apps/modia/hooks/useHentVeilederdata";
import { useInitializeModiaContext } from "@/apps/modia/hooks/useInitializeModiaContext";
import { useInitializeArbeidsmarkedstiltakFilterForBruker } from "@/apps/modia/hooks/useInitializeArbeidsmarkedstiltakFilterForBruker";
import { useFeatureToggle } from "@/core/api/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";
import { AppContainer } from "@/layouts/AppContainer";
import { DemoImageHeader } from "@/components/DemoImageHeader";
import { Landingsside } from "./views/Landingsside";
import { ModiaArbeidsmarkedstiltakOversikt } from "./views/ModiaArbeidsmarkedstiltakOversikt";
import { ModiaArbeidsmarkedstiltakDetaljer } from "./views/ModiaArbeidsmarkedstiltakDetaljer";

export function ModiaArbeidsmarkedstiltak() {
  return (
    <AppContainer header={<DemoImageHeader />}>
      <ModiaArbeidsmarkedstiltakRoutes />
    </AppContainer>
  );
}

function ModiaArbeidsmarkedstiltakRoutes() {
  useHentVeilederdata(); // Pre-fetch veilederdata så slipper vi å vente på data når vi trenger det i appen senere

  useInitializeModiaContext();

  useInitializeArbeidsmarkedstiltakFilterForBruker();

  const enableLandingssideFeature = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE,
  );
  const enableLandingsside = enableLandingssideFeature.isSuccess && enableLandingssideFeature.data;

  if (enableLandingssideFeature.isLoading) {
    return null;
  }

  return (
    <Routes>
      {enableLandingsside ? <Route path="" element={<Landingsside />} /> : null}
      <Route path="oversikt" element={<ModiaArbeidsmarkedstiltakOversikt />} />
      <Route path="tiltak/:id/*" element={<ModiaArbeidsmarkedstiltakDetaljer />} />
      <Route
        path="*"
        element={
          <Navigate replace to={enableLandingsside ? "/arbeidsmarkedstiltak" : "./oversikt"} />
        }
      />
    </Routes>
  );
}
