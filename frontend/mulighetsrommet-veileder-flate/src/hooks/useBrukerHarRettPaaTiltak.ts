import { Innsatsgruppe } from "mulighetsrommet-api-client";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { utledInnsatsgrupperFraInnsatsgruppe } from "../core/api/queries/useTiltaksgjennomforinger";
import useTiltaksgjennomforingById from "../core/api/queries/useTiltaksgjennomforingById";

export function useBrukerHarRettPaaTiltak() {
  const { data } = useTiltaksgjennomforingById();
  const { data: brukerdata } = useHentBrukerdata();
  const innsatsgruppeForGjennomforing =
    data?.tiltakstype?.innsatsgruppe?.nokkel ?? Innsatsgruppe.STANDARD_INNSATS;
  const brukersInnsatsgruppe = brukerdata?.innsatsgruppe;

  const godkjenteInnsatsgrupper = brukersInnsatsgruppe
    ? utledInnsatsgrupperFraInnsatsgruppe(brukersInnsatsgruppe)
    : [];

  const brukerHarRettPaaTiltak = godkjenteInnsatsgrupper.includes(innsatsgruppeForGjennomforing);

  return {
    brukerHarRettPaaTiltak,
    brukersInnsatsgruppe,
    innsatsgruppeForGjennomforing,
  };
}
