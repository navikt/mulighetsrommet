import { useAtom } from "jotai";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { useInnsatsgrupper } from "../core/api/queries/useInnsatsgrupper";
import { ApentForInnsok, HarDeltMedBruker } from "mulighetsrommet-api-client";
import { tiltaksgjennomforingsfilter } from "../core/atoms/atoms";

export function usePrepopulerFilter() {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
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
            harDeltMedBruker: HarDeltMedBruker.HAR_ELLER_HAR_IKKE_DELT,
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
