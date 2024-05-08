import { avtaleHandlers } from "./endpoints/avtaleHandlers";
import { brukerHandlers } from "./endpoints/brukerHandlers";
import { delMedBrukerHandlers } from "./endpoints/delMedBrukerHandlers";
import { enhetHandlers } from "./endpoints/enheterHandlers";
import { featureToggleHandlers } from "./endpoints/featureToggleHandlers";
import { joyrideHandlers } from "./endpoints/joyrideHandlers";
import { oppskriftHandlers } from "./endpoints/oppskriftHandlers";
import { tiltakHandlers } from "./endpoints/tiltakHandlers";
import { veilederHandlers } from "./endpoints/veilederHandlers";

export const apiHandlers = [
  ...tiltakHandlers,
  ...avtaleHandlers,
  ...oppskriftHandlers,
  ...delMedBrukerHandlers,
  ...brukerHandlers,
  ...veilederHandlers,
  ...featureToggleHandlers,
  ...enhetHandlers,
  ...joyrideHandlers,
];
