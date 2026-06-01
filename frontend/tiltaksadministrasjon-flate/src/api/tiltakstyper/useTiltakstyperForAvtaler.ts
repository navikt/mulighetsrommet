import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import {
  SortDirection,
  TiltakstypeEgenskap,
  TiltakstypeSortField,
} from "@tiltaksadministrasjon/api-client";

export function useTiltakstyperForAvtaler() {
  return useTiltakstyper({
    sort: { field: TiltakstypeSortField.NAVN, direction: SortDirection.ASC },
    egenskaper: [TiltakstypeEgenskap.STOTTER_AVTALER],
  });
}
