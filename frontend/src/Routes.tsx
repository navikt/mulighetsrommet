import React from 'react';
import { Route, Switch } from 'react-router-dom';
import TiltaksgjennomforingDetaljer from './views/tiltaksgjennomforing-detaljer/TiltaksgjennomforingDetaljer';
import TiltakstypeDetaljer from './views/tiltakstype-detaljer/TiltakstypeDetaljer';
import TiltakstypeOversikt from './views/tiltakstype-oversikt/TiltakstypeOversikt';

const Routes = () => {
  return (
    <Switch>
      <Route exact path="/tiltakstyper/:tiltakskode" component={TiltakstypeDetaljer} />
      <Route
        exact
        path="/tiltakstyper/:tiltakstypeId/tiltaksgjennomforinger/:tiltaksgjennomforingsId"
        component={TiltaksgjennomforingDetaljer}
      />
      <Route path="/" component={TiltakstypeOversikt} />
    </Switch>
  );
};

export default Routes;
