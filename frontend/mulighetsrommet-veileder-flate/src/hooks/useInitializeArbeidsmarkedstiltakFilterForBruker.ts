import { useEffect } from "react";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { useInnsatsgrupper } from "../core/api/queries/useInnsatsgrupper";
import { useArbeidsmarkedstiltakFilter } from "./useArbeidsmarkedstiltakFilter";

export function useInitializeArbeidsmarkedstiltakFilterForBruker() {
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const { data: brukerdata } = useHentBrukerdata();

  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  const brukersInnsatsgruppe = innsatsgrupper?.find(
    (gruppe) => gruppe.nokkel === brukerdata?.innsatsgruppe,
  );

  const resetInnsatsgruppe =
    brukersInnsatsgruppe !== undefined && filter.innsatsgruppe === undefined;

  useEffect(() => {
    if (resetInnsatsgruppe) {
      setFilter({
        ...filter,
        innsatsgruppe: {
          id: brukersInnsatsgruppe.sanityId,
          nokkel: brukersInnsatsgruppe.nokkel,
          tittel: brukersInnsatsgruppe.tittel,
        },
      });
    }
  }, [resetInnsatsgruppe]);
}
