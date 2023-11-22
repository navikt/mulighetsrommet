import { brukerHandlers } from "./endpoints/brukerHandlers";
import { delMedBrukerHandlers } from "./endpoints/delMedBrukerHandlers";
import { enhetHandlers } from "./endpoints/enheterHandlers";
import { featureToggleHandlers } from "./endpoints/featureToggleHandlers";
import { oppskriftHandlers } from "./endpoints/oppskriftHandlers";
import { sanityHandlers } from "./endpoints/sanityHandlers";
import { veilederHandlers } from "./endpoints/veilederHandlers";

export const apiHandlers = [
  ...sanityHandlers,
  ...oppskriftHandlers,
  ...delMedBrukerHandlers,
  ...brukerHandlers,
  ...veilederHandlers,
  ...featureToggleHandlers,
  ...enhetHandlers,
];
