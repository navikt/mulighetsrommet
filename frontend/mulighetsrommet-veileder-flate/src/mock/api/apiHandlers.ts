import { RestHandler } from 'msw';
import { brukerHandlers } from './endpoints/brukerHandlers';
import { delMedBrukerHandlers } from './endpoints/delMedBrukerHandlers';
import { historikkHandlers } from './endpoints/historikkHandlers';
import { sanityHandlers } from './endpoints/sanityHandlers';
import { veilederHandlers } from './endpoints/veilederHandlers';

export const apiHandlers: RestHandler[] = [
  ...sanityHandlers,
  ...delMedBrukerHandlers,
  ...brukerHandlers,
  ...veilederHandlers,
  ...historikkHandlers,
];
