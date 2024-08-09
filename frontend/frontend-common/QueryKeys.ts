import { LagretDokumenttype } from "mulighetsrommet-api-client";

export const QueryKeys = {
  lagredeFilter: (dokumenttype?: LagretDokumenttype) => ["lagrede-filter", dokumenttype],
};
