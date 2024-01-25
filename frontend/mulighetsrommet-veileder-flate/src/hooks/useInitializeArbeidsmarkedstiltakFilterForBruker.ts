import { useEffect } from "react";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { useInnsatsgrupper } from "../core/api/queries/useInnsatsgrupper";
import {
  buildRegionMap,
  useArbeidsmarkedstiltakFilter,
  valgteEnhetsnumre,
} from "./useArbeidsmarkedstiltakFilter";

export function useInitializeArbeidsmarkedstiltakFilterForBruker() {
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const { data: brukerdata } = useHentBrukerdata();

  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  const brukersInnsatsgruppe = innsatsgrupper?.find(
    (gruppe) => gruppe.nokkel === brukerdata?.innsatsgruppe,
  );

  const resetInnsatsgruppe =
    brukersInnsatsgruppe !== undefined && filter.innsatsgruppe === undefined;

  const resetEnheter =
    brukerdata && brukerdata.enheter.length > 0 && valgteEnhetsnumre(filter).length === 0;

  useEffect(() => {
    if (resetInnsatsgruppe || resetEnheter) {
      setFilter({
        ...filter,
        innsatsgruppe: brukersInnsatsgruppe
          ? {
              id: brukersInnsatsgruppe.sanityId,
              nokkel: brukersInnsatsgruppe.nokkel,
              tittel: brukersInnsatsgruppe.tittel,
            }
          : undefined,
        regionMap: buildRegionMap(brukerdata?.enheter ?? []),
      });
    }
  }, [resetInnsatsgruppe]);
}
