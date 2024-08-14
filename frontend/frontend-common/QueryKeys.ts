import { LagretDokumenttype } from "@mr/api-client";

export const QueryKeys = {
  lagredeFilter: (dokumenttype?: LagretDokumenttype) => ["lagrede-filter", dokumenttype],
};
