import React from 'react';
import { Route, Switch } from 'react-router-dom';
import ViewTiltakstypeDetaljer from './views/tiltakstype-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltakstype-oversikt/ViewTiltakstypeOversikt';

const Routes = () => {
  return (
    <Switch>
      <Route exact path="/tiltakstyper/:tiltakskode" component={ViewTiltakstypeDetaljer} />
      //TODO må legge til riktig URL her, dette skal ikke være tiltakskode, men en ID for tiltaksgjennomføring
      {/*<Route exact path="/tiltakstyper/:slug" component={ViewTiltakstypeDetaljer} />*/}
      <Route path="/" exact component={ViewTiltakstypeOversikt} />
    </Switch>
  );
};

export default Routes;
