import { Navigate, Route, Routes } from 'react-router-dom';
import ViewTiltaksgjennomforingDetaljer from './views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljer';
import ViewTiltaksgjennomforingOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltaksgjennomforingOversikt';

const RoutesConfig = () => {
  return (
    <Routes>
      <Route index element={<ViewTiltaksgjennomforingOversikt />} />
      <Route path="tiltak/:tiltaksnummer" element={<ViewTiltaksgjennomforingDetaljer />} />
      <Route path=":tiltaksnummer" element={<Navigate to="/" />} />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
};

export default RoutesConfig;
