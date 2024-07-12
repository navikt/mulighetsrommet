import { ansattHandlers } from "./endpoints/ansattHandler";
import { arrangorHandlers } from "./endpoints/arrangorHandlers";
import { avtaleHandlers } from "./endpoints/avtaleHandlers";
import { enhetHandlers } from "./endpoints/enhetHandlers";
import { featureToggleHandlers } from "./endpoints/featureToggleHandlers";
import { notificationHandlers } from "./endpoints/notificationHandlers";
import { tiltaksgjennomforingHandlers } from "./endpoints/tiltaksgjennomforingHandlers";
import { tiltakstypeHandlers } from "./endpoints/tiltakstyperHandlers";
import { virksomhetHandlers } from "./endpoints/virksomhetHandlers";
import { lagretFilterHandlers } from "./endpoints/lagretFilterHandlers";

export const apiHandlers = [
  ...arrangorHandlers,
  ...tiltakstypeHandlers,
  ...avtaleHandlers,
  ...tiltaksgjennomforingHandlers,
  ...enhetHandlers,
  ...ansattHandlers,
  ...notificationHandlers,
  ...virksomhetHandlers,
  ...featureToggleHandlers,
  ...lagretFilterHandlers,
];
