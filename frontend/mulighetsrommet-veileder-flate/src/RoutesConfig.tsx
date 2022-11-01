import { Navigate, Route, Routes } from 'react-router-dom';
import { useHentFnrFraUrl } from './hooks/useHentFnrFraUrl';
import ViewTiltaksgjennomforingDetaljer from './views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljer';
import ViewTiltakstypeOversikt from './views/tiltaksgjennomforing-oversikt/ViewTiltakstypeOversikt';

const RoutesConfig = () => {
  const fnr = useHentFnrFraUrl();
  return (
    <Routes>
      (
      <>
        <Route path="/" element={<ViewTiltakstypeOversikt />} />
        <Route path="tiltak/:tiltaksnummer" element={<ViewTiltaksgjennomforingDetaljer />} />
        <Route path=":tiltaksnummer" element={<Navigate to={`/${fnr}`} />}></Route>
        {/* Fallback dersom veileder navigerer fra Dialogen til Arbeidsmarkedstiltak */}
      </>
      )
    </Routes>
  );
};

export default RoutesConfig;
