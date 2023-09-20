import { DefaultBodyType, PathParams, rest } from 'msw';
import {
  GetRelevanteTiltaksgjennomforingerForBrukerRequest,
  GetTiltaksgjennomforingForBrukerRequest,
  VeilederflateInnsatsgruppe,
  VeilederflateTiltaksgjennomforing,
  VeilederflateTiltakstype,
} from 'mulighetsrommet-api-client';
import { mockInnsatsgrupper } from '../../fixtures/mockInnsatsgrupper';
import { mockTiltaksgjennomforinger } from '../../fixtures/mockTiltaksgjennomforinger';
import { mockTiltakstyper } from '../../fixtures/mockTiltakstyper';
import { ok } from '../responses';

export const sanityHandlers = [
  rest.get<DefaultBodyType, PathParams, VeilederflateInnsatsgruppe[]>(
    '*/api/v1/internal/sanity/innsatsgrupper',
    async () => {
      return ok(mockInnsatsgrupper);
    }
  ),

  rest.get<DefaultBodyType, PathParams, VeilederflateTiltakstype[]>(
    '*/api/v1/internal/sanity/tiltakstyper',
    async () => {
      return ok(mockTiltakstyper);
    }
  ),

  rest.post<DefaultBodyType, PathParams, any>('*/api/v1/internal/sanity/tiltaksgjennomforinger', async req => {
    const {
      innsatsgruppe = '',
      search = '',
      tiltakstypeIds = [],
    } = await req.json<GetRelevanteTiltaksgjennomforingerForBrukerRequest>();

    const results = mockTiltaksgjennomforinger
      .filter(gj => filtrerFritekst(gj, search))
      .filter(gj => filtrerInnsatsgruppe(gj, innsatsgruppe))
      .filter(gj => filtrerTiltakstyper(gj, tiltakstypeIds));

    return ok(results);
  }),

  rest.post<DefaultBodyType, PathParams, any>('*/api/v1/internal/sanity/tiltaksgjennomforing', async req => {
    const { sanityId } = await req.json<GetTiltaksgjennomforingForBrukerRequest>();
    const gjennomforing = mockTiltaksgjennomforinger.find(gj => gj.sanityId === sanityId);
    return ok(gjennomforing);
  }),

  rest.get<DefaultBodyType, PathParams, any>('*/api/v1/internal/sanity/tiltaksgjennomforing/preview/:id', async req => {
    const id = req.params.id;
    const gjennomforing = mockTiltaksgjennomforinger.find(gj => gj.sanityId === id);
    return ok(gjennomforing);
  }),
];

function filtrerFritekst(gjennomforing: VeilederflateTiltaksgjennomforing, sok: string): boolean {
  return sok === '' || gjennomforing.navn.toLocaleLowerCase().includes(sok.toLocaleLowerCase());
}

function filtrerInnsatsgruppe(gjennomforing: VeilederflateTiltaksgjennomforing, innsatsgruppe: string): boolean {
  return innsatsgruppe === '' || gjennomforing.tiltakstype.innsatsgruppe.nokkel === innsatsgruppe;
}

function filtrerTiltakstyper(gjennomforing: VeilederflateTiltaksgjennomforing, tiltakstyper: string[]): boolean {
  return tiltakstyper.length === 0 || tiltakstyper.includes(gjennomforing.tiltakstype.sanityId);
}
