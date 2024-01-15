import { useAtom } from "jotai";
import { useEffect } from "react";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { useInnsatsgrupper } from "../core/api/queries/useInnsatsgrupper";
import { tiltaksgjennomforingsfilter } from "../core/atoms/atoms";

export function useInitializeArbeidsmarkedstiltakFilter() {
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const { data: brukerdata } = useHentBrukerdata();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  const brukersInnsatsgruppe = innsatsgrupper?.find(
    (gruppe) => gruppe.nokkel === brukerdata?.innsatsgruppe,
  );

  useEffect(() => {
    if (innsatsgrupper && brukersInnsatsgruppe && !filter.innsatsgruppe) {
      setFilter({
        ...filter,
        innsatsgruppe: {
          id: brukersInnsatsgruppe.sanityId,
          nokkel: brukersInnsatsgruppe.nokkel,
          tittel: brukersInnsatsgruppe.tittel,
        },
      });
    }
  }, [innsatsgrupper, brukersInnsatsgruppe]);
}
