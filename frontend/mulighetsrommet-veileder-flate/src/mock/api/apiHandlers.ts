import { brukerHandlers } from "./endpoints/brukerHandlers";
import { delMedBrukerHandlers } from "./endpoints/delMedBrukerHandlers";
import { enhetHandlers } from "./endpoints/enheterHandlers";
import { joyrideHandlers } from "./endpoints/joyrideHandlers";
import { lagretFilterHandlers } from "./endpoints/lagretFilterHandlers";
import { oppskriftHandlers } from "./endpoints/oppskriftHandlers";
import { tiltakHandlers } from "./endpoints/tiltakHandlers";
import { veilederHandlers } from "./endpoints/veilederHandlers";

export const apiHandlers = [
  ...tiltakHandlers,
  ...oppskriftHandlers,
  ...delMedBrukerHandlers,
  ...brukerHandlers,
  ...veilederHandlers,
  ...enhetHandlers,
  ...joyrideHandlers,
  ...lagretFilterHandlers,
];
