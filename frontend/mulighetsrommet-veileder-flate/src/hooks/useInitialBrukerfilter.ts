import { useAtom } from "jotai";
import { useEffect } from "react";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { useInnsatsgrupper } from "../core/api/queries/useInnsatsgrupper";
import { tiltaksgjennomforingsfilter } from "../core/atoms/atoms";

export function useInitialBrukerfilter() {
  const brukerdata = useHentBrukerdata();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const matchedInnsatsgruppe = innsatsgrupper?.find(
    (gruppe) => gruppe.nokkel === brukerdata?.data?.innsatsgruppe,
  );
  const data = brukerdata?.data;

  useEffect(() => {
    if (data?.innsatsgruppe && innsatsgrupper && matchedInnsatsgruppe && !filter.innsatsgruppe) {
      setFilter({
        ...filter,
        innsatsgruppe: {
          id: matchedInnsatsgruppe.sanityId,
          nokkel: matchedInnsatsgruppe.nokkel,
          tittel: matchedInnsatsgruppe.tittel,
        },
      });
    }
  }, [data, innsatsgrupper]);
}
