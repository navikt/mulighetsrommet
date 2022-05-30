import React from 'react';
import { Route, Routes } from 'react-router-dom';
import ViewTiltakstypeDetaljer from './views/tiltakstype-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltakstype-oversikt/ViewTiltakstypeOversikt';

const RoutesConfig = () => {
  return (
    <Routes>
      <Route path="/" element={<ViewTiltakstypeOversikt />} />
      <Route path="/tiltakstyper/:tiltakskode" element={<ViewTiltakstypeDetaljer />} />
    </Routes>
  );
};

export default RoutesConfig;
