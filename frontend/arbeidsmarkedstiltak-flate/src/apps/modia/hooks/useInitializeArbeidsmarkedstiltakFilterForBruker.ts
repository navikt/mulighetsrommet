import { useEffect, useRef } from "react";
import { useBrukerdata } from "@/apps/modia/hooks/useBrukerdata";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useInnsatsgrupper } from "@/api/queries/useInnsatsgrupper";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";

export function useInitializeArbeidsmarkedstiltakFilterForBruker() {
  const { fnr } = useModiaContext();
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const { data: brukerdata } = useBrukerdata();

  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  const initializedForFnrRef = useRef<string | null>(null);

  const brukersInnsatsgruppe = innsatsgrupper.find(
    (gruppe) => gruppe.nokkel === brukerdata.innsatsgruppe,
  );

  useEffect(() => {
    if (initializedForFnrRef.current === fnr) {
      return;
    }

    const resetInnsatsgruppe =
      brukersInnsatsgruppe !== undefined && filter.innsatsgruppe === undefined;

    const resetEnheter = brukerdata.enheter.length > 0 && filter.navEnheter.length === 0;

    if (resetInnsatsgruppe || resetEnheter) {
      setFilter({
        ...filter,
        innsatsgruppe: brukersInnsatsgruppe
          ? {
              nokkel: brukersInnsatsgruppe.nokkel,
              tittel: brukersInnsatsgruppe.tittel,
            }
          : undefined,
        navEnheter: brukerdata.enheter.map((enhet) => enhet.enhetsnummer),
        erSykmeldtMedArbeidsgiver: brukerdata.erSykmeldtMedArbeidsgiver,
      });
    }

    initializedForFnrRef.current = fnr;
  }, [
    fnr,
    setFilter,
    filter,
    brukersInnsatsgruppe,
    brukerdata.enheter,
    brukerdata.erSykmeldtMedArbeidsgiver,
  ]);
}
