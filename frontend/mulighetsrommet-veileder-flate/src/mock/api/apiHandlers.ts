import { createClient, SanityClient } from '@sanity/client';
import groq from 'groq';
import { rest, RestHandler } from 'msw';
import {
  Ansatt,
  Bruker,
  DelMedBruker,
  DialogResponse,
  HistorikkForBruker,
  Innsatsgruppe,
} from 'mulighetsrommet-api-client';
import { utledInnsatsgrupperFraInnsatsgruppe } from '../../core/api/queries/useTiltaksgjennomforinger';
import { historikk } from '../fixtures/historikk';
import { badReq, ok } from './responses';

const ENHET_FREDRIKSTAD = '0106';
const FYLKE_NAV_OST_VIKEN = '0200';

export const apiHandlers: RestHandler[] = [
  rest.get<any, any, Bruker>('*/api/v1/internal/bruker', (req, res, ctx) => {
    const fnr = req.url.searchParams.get('fnr');

    if (!fnr) {
      return badReq("'fnr' must be specified");
    }

    return res(
      ctx.status(200),
      ctx.json({
        fnr,
        //En bruker har enten servicegruppe eller innsatsgruppe. Denne kan endres ved behov
        innsatsgruppe: Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        // servicegruppe: 'BATT',
        oppfolgingsenhet: {
          navn: 'NAV Fredrikstad',
          enhetId: ENHET_FREDRIKSTAD,
        },
        fornavn: 'IHERDIG',
        geografiskEnhet: {
          navn: 'NAV Fredrikstad',
          enhetsnummer: ENHET_FREDRIKSTAD,
        },
        manuellStatus: {
          erUnderManuellOppfolging: false,
          krrStatus: {
            kanVarsles: true,
            erReservert: false,
          },
        },
      })
    );
  }),

  rest.get<any, any, Ansatt>('*/api/v1/internal/ansatt/me', (_, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        etternavn: 'VEILEDERSEN',
        fornavn: 'VEILEDER',
        ident: 'V12345',
        navn: 'Veiledersen, Veileder',
        tilganger: [],
        hovedenhet: '2990',
        hovedenhetNavn: 'Østfold',
      })
    );
  }),

  rest.post<any, any, DialogResponse>('*/api/v1/internal/dialog', (_, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        id: '12345',
      })
    );
  }),

  rest.get<any, any, any>('*/api/v1/internal/sanity', async req => {
    const query = req.url.searchParams.get('query');

    if (!query) {
      return badReq("'query' must be specified");
    }

    const client = getSanityClient();
    const result = await client.fetch(query, {
      enhetsId: `enhet.lokal.${ENHET_FREDRIKSTAD}`,
      fylkeId: `enhet.fylke.${FYLKE_NAV_OST_VIKEN}`,
    });
    return ok(result);
  }),

  rest.get<any, any, any>('*/api/v1/internal/sanity/innsatsgrupper', async () => {
    const query = groq`*[_type == "innsatsgruppe" && !(_id in path("drafts.**"))]`;
    const client = getSanityClient();
    const result = await client.fetch(query);
    return ok(result);
  }),

  rest.get<any, any, any>('*/api/v1/internal/sanity/tiltakstyper', async () => {
    const query = groq`*[_type == "tiltakstype" && !(_id in path("drafts.**"))]`;
    const client = getSanityClient();
    const result = await client.fetch(query);
    return ok(result);
  }),

  rest.get<any, any, any>('*/api/v1/internal/sanity/lokasjoner', async () => {
    const query = groq`array::unique(*[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**"))
    ${enhetOgFylkeFilter()}]
    {
      lokasjon
    }.lokasjon)`;
    const client = getSanityClient();
    const result = await client.fetch(query);
    return ok(result);
  }),

  rest.get<any, any, any>('*/api/v1/internal/sanity/tiltaksgjennomforinger', async req => {
    const innsatsgruppe = req.url.searchParams.get('innsatsgruppe') || '';
    const tiltakstypeIder = req.url.searchParams.getAll('tiltakstypeIder');
    const sokestreng = req.url.searchParams.get('sokestreng') || '';
    const lokasjoner = req.url.searchParams.getAll('lokasjoner');
    const sanityQueryString = groq`*[_type == "tiltaksgjennomforing" && !(_id in path("drafts.**"))
    ${byggInnsatsgruppeFilter(innsatsgruppe)}
    ${byggTiltakstypeFilter(tiltakstypeIder)}
    ${byggSokefilter(sokestreng)}
    ${byggLokasjonsFilter(lokasjoner)}
    ${enhetOgFylkeFilter()}
    ]
    {
      _id,
      tiltaksgjennomforingNavn,
      lokasjon,
      oppstart,
      oppstartsdato,
      estimert_ventetid,
      "tiltaksnummer": tiltaksnummer.current,
      kontaktinfoArrangor->{selskapsnavn},
      tiltakstype->{tiltakstypeNavn},
      tilgjengelighetsstatus
    }`;

    const client = getSanityClient();
    const result = await client.fetch(sanityQueryString);
    return ok(result);
  }),

  rest.get<any, any, any>('*/api/v1/internal/sanity/tiltaksgjennomforing/:id', async req => {
    const id = req.params.id;
    const matchIdForProdEllerDrafts = `(_id == '${id}' || _id == 'drafts.${id}')`;
    const query = groq`*[_type == "tiltaksgjennomforing" && ${matchIdForProdEllerDrafts}] {
    _id,
    tiltaksgjennomforingNavn,
    beskrivelse,
    "tiltaksnummer": tiltaksnummer.current,
    tilgjengelighetsstatus,
    estimert_ventetid,
    lokasjon,
    oppstart,
    oppstartsdato,
    sluttdato,
    faneinnhold {
      forHvemInfoboks,
      forHvem,
      detaljerOgInnholdInfoboks,
      detaljerOgInnhold,
      pameldingOgVarighetInfoboks,
      pameldingOgVarighet,
    },
    kontaktinfoArrangor->,
    kontaktinfoTiltaksansvarlige[]->,
    tiltakstype->{
      ...,
      regelverkFiler[]-> {
        _id,
        "regelverkFilUrl": regelverkFilOpplastning.asset->url,
        regelverkFilNavn
      },
      regelverkLenker[]->,
      innsatsgruppe->,

    }
  }`;

    const client = getSanityClient();
    const result = await client.fetch(query);
    return ok(result);
  }),

  rest.get<any, any, HistorikkForBruker[]>('*/api/v1/internal/bruker/historikk', (_, res, ctx) => {
    return res(ctx.status(200), ctx.json(historikk));
  }),

  rest.post<DelMedBruker, any, DelMedBruker>('*/api/v1/internal/delMedBruker', async (req, res, ctx) => {
    const data = (await req.json()) as DelMedBruker;
    return res(ctx.status(200), ctx.json(data));
  }),

  rest.get<any, any, DelMedBruker>('*/api/v1/internal/delMedBruker/*', (_, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        tiltaksnummer: '29518',
        navident: 'V15555',
        dialogId: '12345',
        bruker_fnr: '11223344557',
        createdAt: new Date(2022, 2, 22).toString(),
      })
    );
  }),
];

let cachedClient: SanityClient | null = null;

function getSanityClient() {
  if (cachedClient) {
    return cachedClient;
  }

  cachedClient = createClient({
    apiVersion: '2022-06-20',
    projectId: import.meta.env.VITE_SANITY_PROJECT_ID,
    dataset: import.meta.env.VITE_SANITY_DATASET,
  });

  return cachedClient;
}

function byggLokasjonsFilter(lokasjoner: string[]): string {
  if (lokasjoner.length === 0) return '';

  const lokasjonsStreng = fnuttifiserListe(lokasjoner);

  return groq`&& lokasjon in [${lokasjonsStreng}]`;
}

function byggInnsatsgruppeFilter(innsatsgruppe?: string): string {
  if (!innsatsgruppe) return '';

  const innsatsgrupperISok = utledInnsatsgrupperFraInnsatsgruppe(innsatsgruppe)
    .map(nokkel => `"${nokkel}"`)
    .join(', ');
  return groq`&& tiltakstype->innsatsgruppe->nokkel in [${innsatsgrupperISok}]`;
}

function byggTiltakstypeFilter(tiltakstyper: string[]): string {
  return tiltakstyper.length > 0 ? groq`&& tiltakstype->_id in [${fnuttifiserListe(tiltakstyper)}]` : '';
}

function byggSokefilter(search: string) {
  return search
    ? groq`&& [tiltaksgjennomforingNavn, string(tiltaksnummer.current), tiltakstype->tiltakstypeNavn, lokasjon, kontaktinfoArrangor->selskapsnavn, oppstartsdato] match "*${search}*"`
    : '';
}

function fnuttifiserListe(elementer: string[]): string {
  return elementer.map(it => `"${it}"`).join(', ');
}

function enhetOgFylkeFilter() {
  const enhetLokal = `enhet.lokal.${ENHET_FREDRIKSTAD}`;
  const fylke = `enhet.fylke.${FYLKE_NAV_OST_VIKEN}`;
  return groq`&& ('${enhetLokal}' in enheter[]._ref || (enheter[0] == null && '${fylke}' == fylke._ref))
  && ('${enhetLokal}' in enheter[]._ref || (enheter[0] == null && '${fylke}' == fylke._ref))`;
}
