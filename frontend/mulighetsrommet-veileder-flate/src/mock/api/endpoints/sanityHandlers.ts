import groq from 'groq';
import { DefaultBodyType, PathParams, rest } from 'msw';
import { utledInnsatsgrupperFraInnsatsgruppe } from '../../../core/api/queries/useTiltaksgjennomforinger';
import { badReq, ok } from '../responses';
import { SanityClient, ClientConfig, createClient } from '@sanity/client';
import { ENHET_FREDRIKSTAD, FYLKE_NAV_OST_VIKEN } from '../../mock_constants';
import { SanityInnsatsgruppe, SanityTiltakstype } from 'mulighetsrommet-api-client';

let cachedClient: SanityClient | null = null;

function getSanityClient(perspective: ClientConfig['perspective'] = 'published') {
  if (cachedClient) {
    return cachedClient;
  }

  cachedClient = createClient({
    apiVersion: '2023-07-10',
    projectId: import.meta.env.VITE_SANITY_PROJECT_ID,
    dataset: import.meta.env.VITE_SANITY_DATASET,
    perspective,
  });

  return cachedClient;
}

export const sanityHandlers = [
  rest.get<DefaultBodyType, PathParams, SanityInnsatsgruppe[]>('*/api/v1/internal/sanity/innsatsgrupper', async () => {
    const query = groq`*[_type == "innsatsgruppe"]`;
    const client = getSanityClient();
    const result = await client.fetch<SanityInnsatsgruppe[]>(query);
    return ok(result);
  }),

  rest.get<DefaultBodyType, PathParams, SanityTiltakstype[]>('*/api/v1/internal/sanity/tiltakstyper', async () => {
    const query = groq`*[_type == "tiltakstype"]`;
    const client = getSanityClient();
    const result = await client.fetch<SanityTiltakstype[]>(query);
    return ok(result);
  }),

  rest.get<DefaultBodyType, PathParams, any>('*/api/v1/internal/sanity/lokasjoner', async () => {
    const query = groq`array::unique(*[_type == "tiltaksgjennomforing"
    ${enhetOgFylkeFilter()}]
    {
      lokasjon
    }.lokasjon)`;
    const client = getSanityClient();
    const result = await client.fetch(query);
    return ok(result);
  }),

  rest.get<DefaultBodyType, PathParams, any>('*/api/v1/internal/sanity/tiltaksgjennomforinger', async req => {
    const innsatsgruppe = req.url.searchParams.get('innsatsgruppe') || '';
    const tiltakstypeIder = req.url.searchParams.getAll('tiltakstypeIder');
    const sokestreng = req.url.searchParams.get('sokestreng') || '';
    const lokasjoner = req.url.searchParams.getAll('lokasjoner');
    const sanityQueryString = groq`*[_type == "tiltaksgjennomforing"
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

  rest.get<DefaultBodyType, PathParams, any>('*/api/v1/internal/sanity/tiltaksgjennomforing/:id', async req => {
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

    const client = getSanityClient('raw');
    const result = await client.fetch(query);
    return ok(result);
  }),
];

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

function enhetOgFylkeFilter() {
  const enhetLokal = `enhet.lokal.${ENHET_FREDRIKSTAD}`;
  const fylke = `enhet.fylke.${FYLKE_NAV_OST_VIKEN}`;
  return groq`&& ('${enhetLokal}' in enheter[]._ref || (enheter[0] == null && '${fylke}' == fylke._ref))
  && ('${enhetLokal}' in enheter[]._ref || (enheter[0] == null && '${fylke}' == fylke._ref))`;
}

function fnuttifiserListe(elementer: string[]): string {
  return elementer.map(it => `"${it}"`).join(', ');
}
