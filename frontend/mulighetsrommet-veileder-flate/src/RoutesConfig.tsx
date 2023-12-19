import { Toggles } from "mulighetsrommet-api-client";
import { Navigate, Route, Routes } from "react-router-dom";
import { Oppskrift } from "./components/oppskrift/Oppskrift";
import { useFeatureToggle } from "./core/api/feature-toggles";
import { DeltakerRegistrering } from "./microfrontends/team_komet/DeltakerRegistrering";
import { routes } from "./routes";
import { Landingsside } from "./views/landingsside/Landingsside";
import { ViewTiltaksgjennomforingDetaljerContainer } from "./views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljerContainer";
import ViewTiltaksgjennomforingOversikt from "./views/tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt";
import { useAppContext } from "./hooks/useAppContext";

const RoutesConfig = () => {
  const { fnr, enhet } = useAppContext();
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
    <Routes>
      {enableLandingsside ? <Route path={routes.base} element={<Landingsside />} /> : null}
      <Route path={routes.detaljer} element={<ViewTiltaksgjennomforingDetaljerContainer />}>
        <Route path={routes.detaljer_oppskrift} element={<Oppskrift />} />
      </Route>
      {visDeltakerregistrering ? (
        <Route
          path={routes.detaljer_deltaker}
          element={<DeltakerRegistrering fnr={fnr} enhetId={enhet} />}
        />
      ) : null}
      <Route path={routes.oversikt} element={<ViewTiltaksgjennomforingOversikt />} />
      <Route
        path="*"
        element={
          <Navigate replace to={enableLandingsside ? `/${routes.base}` : `/${routes.oversikt}`} />
        }
      />
    </Routes>
  );
};

export default RoutesConfig;
