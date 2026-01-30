import { OpprettKravVedleggSteg, OpprettKravVedleggStegGuidePanelType } from "@api-client";
import {
  gjennomforingIdAFT,
  gjennomforingIdAvklaring,
  gjennomforingIdOppfolging,
} from "./gjennomforingMocks";

const forhandsgodkjent: OpprettKravVedleggSteg = {
  guidePanel: OpprettKravVedleggStegGuidePanelType.INVESTERING_VTA_AFT,
  minAntallVedlegg: 1,
};

const annenAvtaltPris: OpprettKravVedleggSteg = {
  guidePanel: OpprettKravVedleggStegGuidePanelType.AVTALT_PRIS,
  minAntallVedlegg: 0,
};

const timespris: OpprettKravVedleggSteg = {
  guidePanel: OpprettKravVedleggStegGuidePanelType.TIMESPRIS,
  minAntallVedlegg: 1,
};

export const vedlegg: Record<string, OpprettKravVedleggSteg> = {
  [gjennomforingIdAFT]: forhandsgodkjent,
  [gjennomforingIdAvklaring]: annenAvtaltPris,
  [gjennomforingIdOppfolging]: timespris,
};
