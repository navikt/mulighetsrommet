import { LagretDokumenttype } from "@mr/api-client-v2";

export const QueryKeys = {
  lagredeFilter: (dokumenttype?: LagretDokumenttype) => ["lagrede-filter", dokumenttype],
};
