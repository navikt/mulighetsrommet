import { Navigate, Route, Routes } from 'react-router-dom';
import { ViewTiltaksgjennomforingDetaljerContainer } from './views/tiltaksgjennomforing-detaljer/viewTiltaksgjennomforingDetaljerContainer';
import ViewTiltaksgjennomforingOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt';

const RoutesConfig = () => {
  return (
    <Routes>
      <Route index element={<ViewTiltaksgjennomforingOversikt />} />
      <Route path="tiltak/:tiltaksnummer" element={<ViewTiltaksgjennomforingDetaljerContainer />} />
      <Route path=":tiltaksnummer" element={<Navigate to="/" />} />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
};

export default RoutesConfig;
