import { createServer, Response } from 'src/mock/MirageJs';
import { innsatsgrupperMock } from './data/InnsatsgrupperMock';
import { tiltaksgjennomforingerMock } from './data/TiltaksgjennomforingerMock';
import { tiltaksvarianterMock } from './data/TiltaksvarianterMock';

const mockServer = () =>
  createServer({
    seeds(server) {
      server.db.loadData({
        innsatsgrupper: innsatsgrupperMock,
        tiltaksvarianter: tiltaksvarianterMock,
        tiltaksgjennomforinger: tiltaksgjennomforingerMock,
      });
    },
    routes() {
      // TODO: Lag en bedre struktur her. Kommer til å bli litt kaos i fremtiden.
      this.namespace = '/api';

      this.get('/innsatsgrupper', schema => {
        return schema.db.innsatsgrupper;
      });

      this.get('/tiltaksvarianter', schema => {
        return schema.db.tiltaksvarianter;
      });
      this.get('/tiltaksvarianter/:id', (schema, request) => {
        const id = request.params.id;
        return schema.db.tiltaksvarianter.find(id);
      });
      this.get('/tiltaksvarianter/:id/tiltaksgjennomforinger', (schema, request) => {
        const id = request.params.id;
        return schema.db.tiltaksgjennomforinger.where({ tiltaksvariantId: id });
      });
      this.post('/tiltaksvarianter', (schema, request) => {
        const tiltaksvarianter = JSON.parse(request.requestBody);
        return schema.db.tiltaksvarianter.insert(tiltaksvarianter);
      });
      this.put('/tiltaksvarianter/:id', (schema, request) => {
        const id = request.params.id;
        const tiltaksvarianter = JSON.parse(request.requestBody);
        return schema.db.tiltaksvarianter.update(id, {
          tittel: tiltaksvarianter.tittel,
          ingress: tiltaksvarianter.ingress,
          beskrivelse: tiltaksvarianter.beskrivelse,
        });
      });
      this.delete('/tiltaksvarianter/:id', (schema, request) => {
        const id = request.params.id;
        const tiltaksvariant = schema.db.tiltaksvarianter.find(id);
        if (tiltaksvariant) {
          schema.db.tiltaksvarianter.remove(id);
          return new Response(200);
        }
        return new Response(404);
      });
      this.get('/tiltaksgjennomforinger', schema => {
        return schema.db.tiltaksgjennomforinger;
      });
      this.get('/tiltaksgjennomforinger/:id', (schema, request) => {
        const id = request.params.id;
        return schema.db.tiltaksgjennomforinger.find(id);
      });
    },
  });

export default mockServer;
