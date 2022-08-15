import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useHentFnrFraUrl } from './hooks/useHentFnrFraUrl';
import ViewTiltakstypeDetaljer from './views/tiltaksgjennomforing-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltakstypeOversikt';
import * as Sentry from '@sentry/react';

const SentryRoutes = Sentry.withSentryReactRouterV6Routing(Routes);

const RoutesConfig = () => {
  const fnr = useHentFnrFraUrl();

  return (
    <SentryRoutes>
      <Route path=":fnr" element={<ViewTiltakstypeOversikt />} />
      <Route path=":fnr/:tiltaksnummer" element={<ViewTiltakstypeDetaljer />} />

      <Route path="*" element={<Navigate to={`${fnr}`} />}>
        {/* Fallback-rute dersom ingenting matcher. Returner bruker til startside */}
      </Route>
    </SentryRoutes>
  );
};

export default RoutesConfig;
