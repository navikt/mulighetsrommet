import React from 'react';
import { Route, Switch } from 'react-router-dom';
import ViewTiltakstypeDetaljer from './views/tiltakstype-detaljer/ViewTiltakstypeDetaljer';
import ViewTiltakstypeOversikt from './views/tiltakstype-oversikt/ViewTiltakstypeOversikt';
import ViewTiltaksgjennomforingDetaljer from './views/tiltaksgjennomforing-detaljer/ViewTiltaksgjennomforingDetaljer';

const Routes = () => {
  return (
    <Switch>
      <Route exact path="/tiltakstyper/:id" component={ViewTiltakstypeDetaljer} />
      <Route
        exact
        path="/tiltakstyper/:tiltakstypeId/tiltaksgjennomforinger/:tiltaksgjennomforingsId"
        component={ViewTiltaksgjennomforingDetaljer}
      />
      <Route path="/" component={ViewTiltakstypeOversikt} />
    </Switch>
  );
};

export default Routes;
