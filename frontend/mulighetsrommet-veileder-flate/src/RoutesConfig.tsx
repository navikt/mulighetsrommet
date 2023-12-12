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
  const { fnr } = useAppContext();
  const enableLandingssideFeature = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE,
  );
  const visDeltakerregistrering = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING,
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
      {visDeltakerregistrering ? (
        <Route
          path={routes.deltaker}
          element={<DeltakerRegistrering fnr={fnr} deltakerliste="" />}
        />
      ) : null}

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
