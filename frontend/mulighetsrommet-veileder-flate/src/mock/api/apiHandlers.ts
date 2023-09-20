import { RestHandler } from 'msw';
import { brukerHandlers } from './endpoints/brukerHandlers';
import { delMedBrukerHandlers } from './endpoints/delMedBrukerHandlers';
import { sanityHandlers } from './endpoints/sanityHandlers';
import { veilederHandlers } from './endpoints/veilederHandlers';
import { featureToggleHandlers } from './endpoints/featureToggleHandlers';

export const apiHandlers: RestHandler[] = [
  ...sanityHandlers,
  ...delMedBrukerHandlers,
  ...brukerHandlers,
  ...veilederHandlers,
  ...featureToggleHandlers,
];
