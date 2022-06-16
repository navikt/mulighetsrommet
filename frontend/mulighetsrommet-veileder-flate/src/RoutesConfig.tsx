import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import ViewTiltakstypeDetaljer from './views/tiltaksgjennomforing-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltakstypeOversikt';

const RoutesConfig = () => {
  return (
    <Routes>
      <Route path="/" element={<ViewTiltakstypeOversikt />} />
      <Route path="/:tiltaksnummer" element={<ViewTiltakstypeDetaljer />} />

      <Route path="*" element={<Navigate to="/" />}>
        {/* Fallback-rute dersom ingenting matcher. Returner bruker til startside */}
      </Route>
    </Routes>
  );
};

export default RoutesConfig;
