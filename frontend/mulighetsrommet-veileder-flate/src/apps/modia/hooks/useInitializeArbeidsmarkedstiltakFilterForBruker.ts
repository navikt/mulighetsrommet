import { useEffect } from "react";
import { useBrukerdata } from "@/apps/modia/hooks/useBrukerdata";
import { useInnsatsgrupper } from "@/api/queries/useInnsatsgrupper";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";

export function useInitializeArbeidsmarkedstiltakFilterForBruker() {
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const { data: brukerdata } = useBrukerdata();

  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  const brukersInnsatsgruppe = innsatsgrupper.find(
    (gruppe) => gruppe.nokkel === brukerdata.innsatsgruppe,
  );

  const resetInnsatsgruppe =
    brukersInnsatsgruppe !== undefined && filter.innsatsgruppe === undefined;

  const resetEnheter = brukerdata.enheter.length > 0 && filter.navEnheter.length === 0;

  useEffect(() => {
    if (resetInnsatsgruppe || resetEnheter) {
      setFilter({
        ...filter,
        innsatsgruppe: brukersInnsatsgruppe
          ? {
              nokkel: brukersInnsatsgruppe.nokkel,
              tittel: brukersInnsatsgruppe.tittel,
            }
          : undefined,
        navEnheter: brukerdata.enheter,
        erSykmeldtMedArbeidsgiver: brukerdata.erSykmeldtMedArbeidsgiver,
      });
    }
  }, [
    resetInnsatsgruppe,
    resetEnheter,
    setFilter,
    filter,
    brukersInnsatsgruppe,
    brukerdata.enheter,
    brukerdata.erSykmeldtMedArbeidsgiver,
  ]);
}
