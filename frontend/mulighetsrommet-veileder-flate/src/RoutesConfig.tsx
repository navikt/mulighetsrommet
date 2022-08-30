import { Route, Routes } from 'react-router-dom';
import ViewTiltakstypeDetaljer from './views/tiltaksgjennomforing-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltakstypeOversikt';

const RoutesConfig = () => {
  return (
    <Routes>
      (
      <>
        <Route path="/" element={<ViewTiltakstypeOversikt />} />
        <Route path="tiltak/:tiltaksnummer" element={<ViewTiltakstypeDetaljer />} />
      </>
      )
    </Routes>
  );
};

export default RoutesConfig;
