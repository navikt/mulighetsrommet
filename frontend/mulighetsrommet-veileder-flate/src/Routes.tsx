import React from 'react';
import { Route, Switch } from 'react-router-dom';
import ViewTiltakstypeDetaljer from './views/tiltakstype-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltakstype-oversikt/ViewTiltakstypeOversikt';

const Routes = () => {
  return (
    <Switch>
      <Route exact path="/:tiltaksnummer" component={ViewTiltakstypeDetaljer} />
      <Route path="/" exact component={ViewTiltakstypeOversikt} />
    </Switch>
  );
};

export default Routes;
