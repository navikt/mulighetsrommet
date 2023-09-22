import { Toggles } from 'mulighetsrommet-api-client';
import { Route, Routes } from 'react-router-dom';
import { useFeatureToggle } from './core/api/feature-toggles';
import { routes } from './routes';
import { Landingsside } from './views/landingsside/Landingsside';
import { ViewTiltaksgjennomforingDetaljerContainer } from './views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljerContainer';
import ViewTiltaksgjennomforingOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt';

const RoutesConfig = () => {
  const enableLandingssideFeature = useFeatureToggle(Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE);
  const enableLandingsside = enableLandingssideFeature.isSuccess && enableLandingssideFeature.data;

  return (
    <Routes>
      {enableLandingsside ? <Route path={routes.base} element={<Landingsside />} /> : null}
      <Route path={routes.detaljer} element={<ViewTiltaksgjennomforingDetaljerContainer />} />
      <Route path={routes.oversikt} element={<ViewTiltaksgjennomforingOversikt />} />
      <Route index element={enableLandingsside ? <Landingsside /> : <ViewTiltaksgjennomforingOversikt />} />
    </Routes>
  );
};

export default RoutesConfig;
