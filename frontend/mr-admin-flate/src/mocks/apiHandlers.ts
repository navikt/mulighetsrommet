import { ansattHandlers } from "./endpoints/ansattHandler";
import { avtaleHandlers } from "./endpoints/avtaleHandlers";
import { enhetHandlers } from "./endpoints/enhetHandlers";
import { notificationHandlers } from "./endpoints/notificationHandlers";
import { tiltaksgjennomforingHandlers } from "./endpoints/tiltaksgjennomforingHandlers";
import { tiltakstypeHandlers } from "./endpoints/tiltakstyperHandlers";
import { virksomhetHandlers } from "./endpoints/virksomhetHandlers";
import { avtalenotatHandlers } from "./endpoints/avtalenotatHandlers";
import { tiltaksgjennomforingNotatHandlers } from "./endpoints/tiltaksgjennomforingNotatHandlers";
import { featureToggleHandlers } from "./endpoints/featureToggleHandlers";

export const apiHandlers = [
  ...tiltakstypeHandlers,
  ...avtaleHandlers,
  ...tiltaksgjennomforingHandlers,
  ...enhetHandlers,
  ...ansattHandlers,
  ...notificationHandlers,
  ...virksomhetHandlers,
  ...avtalenotatHandlers,
  ...tiltaksgjennomforingNotatHandlers,
  ...featureToggleHandlers,
];
