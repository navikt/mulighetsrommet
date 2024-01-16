import { ApentForInnsok } from "mulighetsrommet-api-client";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { useInnsatsgrupper } from "../core/api/queries/useInnsatsgrupper";
import { useArbeidsmarkedstiltakFilter } from "./useArbeidsmarkedstiltakFilter";

export function usePrepopulerFilter() {
  const { filter, setFilter } = useArbeidsmarkedstiltakFilter();
  const brukerdata = useHentBrukerdata();
  const { data: innsatsgrupper } = useInnsatsgrupper();

  function forcePrepopulerFilter(resetFilterTilUtgangspunkt: boolean) {
    const matchedInnsatsgruppe = innsatsgrupper?.find(
      (gruppe) => gruppe.nokkel === brukerdata?.data?.innsatsgruppe,
    );
    if (matchedInnsatsgruppe) {
      const nextFilter = resetFilterTilUtgangspunkt
        ? {
            search: "",
            apentForInnsok: ApentForInnsok.APENT_ELLER_STENGT,
            tiltakstyper: [],
            innsatsgruppe: {
              id: matchedInnsatsgruppe.sanityId,
              nokkel: matchedInnsatsgruppe.nokkel,
              tittel: matchedInnsatsgruppe.tittel,
            },
          }
        : filter;
      setFilter(nextFilter);
    }
  }

  return { forcePrepopulerFilter };
}
