import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useHentFnrFraUrl } from './hooks/useHentFnrFraUrl';
import ViewTiltakstypeDetaljer from './views/tiltaksgjennomforing-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltakstypeOversikt';
import FakeDoor from './components/fakedoor/FakeDoor';
import { ENABLE_ARBEIDSFLATE, useFeatureToggles } from './core/api/feature-toggles';

const RoutesConfig = () => {
  const fnr = useHentFnrFraUrl();
  const features = useFeatureToggles();
  const enableArbeidsflate = features.isSuccess && features.data[ENABLE_ARBEIDSFLATE];

  if (features.isLoading) {
    // Passer på at vi ikke flash-viser løsningen før vi har hentet toggle for fake-door
    return null;
  }

  return (
    <Routes>
      {!enableArbeidsflate ? (
        <Route path=":fnr" element={<FakeDoor />} />
      ) : (
        <>
          <Route path=":fnr" element={<ViewTiltakstypeOversikt />} />
          <Route path=":fnr/:tiltaksnummer" element={<ViewTiltakstypeDetaljer />} />
        </>
      )}
      <Route path="*" element={<Navigate to={`${fnr}`} />}>
        {/* Fallback-rute dersom ingenting matcher. Returner bruker til startside */}
      </Route>
    </Routes>
  );
};

export default RoutesConfig;
