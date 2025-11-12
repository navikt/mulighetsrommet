import {
  OpprettKravVedlegg,
  OpprettKravVedleggGuidePanelType,
  OpprettKravVeiviserSteg,
} from "@api-client";
import {
  gjennomforingIdAFT,
  gjennomforingIdAvklaring,
  gjennomforingIdOppfolging,
} from "./gjennomforingMocks";

const forhandsgodkjent: OpprettKravVedlegg = {
  guidePanel: OpprettKravVedleggGuidePanelType.INVESTERING_VTA_AFT,
  minAntallVedlegg: 1,
  navigering: {
    tilbake: OpprettKravVeiviserSteg.UTBETALING,
    neste: OpprettKravVeiviserSteg.OPPSUMMERING,
  },
};

const annenAvtaltPris: OpprettKravVedlegg = {
  guidePanel: OpprettKravVedleggGuidePanelType.AVTALT_PRIS,
  minAntallVedlegg: 0,
  navigering: {
    tilbake: OpprettKravVeiviserSteg.UTBETALING,
    neste: OpprettKravVeiviserSteg.OPPSUMMERING,
  },
};

const timespris: OpprettKravVedlegg = {
  guidePanel: OpprettKravVedleggGuidePanelType.TIMESPRIS,
  minAntallVedlegg: 1,
  navigering: {
    tilbake: OpprettKravVeiviserSteg.UTBETALING,
    neste: OpprettKravVeiviserSteg.OPPSUMMERING,
  },
};

export const vedlegg: Record<string, OpprettKravVedlegg> = {
  [gjennomforingIdAFT]: forhandsgodkjent,
  [gjennomforingIdAvklaring]: annenAvtaltPris,
  [gjennomforingIdOppfolging]: timespris,
};
