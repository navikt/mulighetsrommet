import { DefaultBodyType, PathParams, rest } from 'msw';
import {
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

  rest.get<DefaultBodyType, PathParams, any>('*/api/v1/internal/sanity/tiltaksgjennomforinger', async req => {
    const innsatsgruppe = req.url.searchParams.get('innsatsgruppe') || '';
    const tiltakstypeIder = req.url.searchParams.getAll('tiltakstypeIder');
    const sokestreng = req.url.searchParams.get('sokestreng') || '';
    const results = mockTiltaksgjennomforinger
      .filter(gj => filtrerFritekst(gj, sokestreng))
      .filter(gj => filtrerInnsatsgruppe(gj, innsatsgruppe))
      .filter(gj => filtrerTiltakstyper(gj, tiltakstypeIder));

    return ok(results);
  }),

  rest.get<DefaultBodyType, PathParams, any>('*/api/v1/internal/sanity/tiltaksgjennomforing/:id', async req => {
    const id = req.params.id;
    const gjennomforing = mockTiltaksgjennomforinger.find(gj => gj.sanityId === id);
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
