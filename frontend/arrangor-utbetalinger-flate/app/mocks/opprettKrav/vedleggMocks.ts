import {
  OpprettKravVedleggSteg,
  OpprettKravVedleggStegGuidePanelType,
} from "@arrangor-utbetalinger/api-client";
import { gjennomforingIdAFT, gjennomforingIdAvklaring } from "./gjennomforingMocks";

const forhandsgodkjent: OpprettKravVedleggSteg = {
  guidePanel: OpprettKravVedleggStegGuidePanelType.INVESTERING_VTA_AFT,
  minAntallVedlegg: 1,
};

const annenAvtaltPris: OpprettKravVedleggSteg = {
  guidePanel: OpprettKravVedleggStegGuidePanelType.AVTALT_PRIS,
  minAntallVedlegg: 0,
};

export const vedlegg: Record<string, OpprettKravVedleggSteg> = {
  [gjennomforingIdAFT]: forhandsgodkjent,
  [gjennomforingIdAvklaring]: annenAvtaltPris,
};
