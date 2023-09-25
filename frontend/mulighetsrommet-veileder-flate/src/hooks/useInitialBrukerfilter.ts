import { useEffect } from "react";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { useInnsatsgrupper } from "../core/api/queries/useInnsatsgrupper";
import { usePrepopulerFilter } from "./usePrepopulerFilter";

export function useInitialBrukerfilter() {
  const brukerdata = useHentBrukerdata();
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const data = brukerdata?.data;

  useEffect(() => {
    if (data && innsatsgrupper) {
      forcePrepopulerFilter(true);
    }
  }, [data, innsatsgrupper]);
}
