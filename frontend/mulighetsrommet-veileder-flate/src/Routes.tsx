import React from 'react';
import { Route, Switch, X } from 'react-router-dom';
import ViewTiltakstypeDetaljer from './views/tiltakstype-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltakstype-oversikt/ViewTiltakstypeOversikt';

const Routes = () => {
  return (
    <Switch>
      <Route exact path="/tiltakstyper/:tiltakskode" component={ViewTiltakstypeDetaljer} />
      <Route path="/" component={ViewTiltakstypeOversikt} />
    </Switch>
  );
};

export default Routes;
