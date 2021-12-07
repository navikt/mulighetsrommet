import React from 'react';
import { Route, Switch } from 'react-router-dom';
import TiltaksgjennomforingDetaljer from './views/tiltaksgjennomforing-detaljer/TiltaksgjennomforingDetaljer';
import TiltaksvariantDetaljer from './views/tiltaksvariant-detaljer/TiltaksvariantDetaljer';
import TiltaksvariantOversikt from './views/tiltaksvariant-oversikt/TiltaksvariantOversikt';
import { CreateTiltaksvariant } from './views/tiltaksvariant-redigering/CreateTiltaksvariant';
import { EditTiltaksvariant } from './views/tiltaksvariant-redigering/EditTiltaksvariant';

const Routes = () => {
  return (
    <Switch>
      <Route exact path="/tiltaksvarianter/opprett" component={CreateTiltaksvariant} />
      <Route exact path="/tiltaksvarianter/:id" component={TiltaksvariantDetaljer} />
      <Route exact path="/tiltaksvarianter/:id/rediger" component={EditTiltaksvariant} />
      <Route
        exact
        path="/tiltaksvarianter/:tiltaksvariantId/tiltaksgjennomforinger/:tiltaksgjennomforingsId"
        component={TiltaksgjennomforingDetaljer}
      />
      <Route path="/" component={TiltaksvariantOversikt} />
    </Switch>
  );
};

export default Routes;
