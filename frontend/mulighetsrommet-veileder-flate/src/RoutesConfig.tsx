import { Toggles } from 'mulighetsrommet-api-client';
import { Route, Routes } from 'react-router-dom';
import { useFeatureToggle } from './core/api/feature-toggles';
import { Landingsside } from './views/landingsside/Landingsside';
import { ViewTiltaksgjennomforingDetaljerContainer } from './views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljerContainer';
import ViewTiltaksgjennomforingOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt';

const RoutesConfig = () => {
  const enableLandingssideFeature = useFeatureToggle(Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE);
  const enableLandingsside = enableLandingssideFeature.isSuccess && enableLandingssideFeature.data;
  return (
    <Routes>
      <Route path="tiltak/:tiltaksnummer" element={<ViewTiltaksgjennomforingDetaljerContainer />} />
      <Route path="oversikt" element={<ViewTiltaksgjennomforingOversikt />} />
      {enableLandingsside ? <Route index element={<Landingsside />} /> : null}
      <Route path="*" element={<ViewTiltaksgjennomforingOversikt />} />
    </Routes>
  );
};

export default RoutesConfig;
