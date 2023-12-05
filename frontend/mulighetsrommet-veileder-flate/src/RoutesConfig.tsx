import { Toggles } from "mulighetsrommet-api-client";
import { Navigate, Route, Routes } from "react-router-dom";
import { useFeatureToggle } from "./core/api/feature-toggles";
import { routes } from "./routes";
import { Landingsside } from "./views/landingsside/Landingsside";
import { ViewTiltaksgjennomforingDetaljerContainer } from "./views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljerContainer";
import ViewTiltaksgjennomforingOversikt from "./views/tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt";
import { Oppskrift } from "./components/oppskrift/Oppskrift";

const RoutesConfig = () => {
  const enableLandingssideFeature = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE,
  );
  const enableLandingsside = enableLandingssideFeature.isSuccess && enableLandingssideFeature.data;

  if (enableLandingssideFeature.isLoading) {
    return null;
  }

  return (
    <Routes>
      {enableLandingsside ? <Route path={routes.base} element={<Landingsside />} /> : null}
      <Route path={routes.detaljer} element={<ViewTiltaksgjennomforingDetaljerContainer />}>
        <Route path={routes.detaljer_oppskrift} element={<Oppskrift />} />
      </Route>
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
