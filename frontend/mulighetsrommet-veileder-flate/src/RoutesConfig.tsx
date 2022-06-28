import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import ViewTiltakstypeDetaljer from './views/tiltaksgjennomforing-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltakstypeOversikt';

const RoutesConfig = () => {
  return (
    <Routes>
      <Route path="oversikt" element={<ViewTiltakstypeOversikt />} />
      <Route path="oversikt/:tiltaksnummer" element={<ViewTiltakstypeDetaljer />} />

      <Route path="*" element={<Navigate to={`oversikt`} />}>
        {/* Fallback-rute dersom ingenting matcher. Returner bruker til startside */}
      </Route>
    </Routes>
  );
};

export default RoutesConfig;
