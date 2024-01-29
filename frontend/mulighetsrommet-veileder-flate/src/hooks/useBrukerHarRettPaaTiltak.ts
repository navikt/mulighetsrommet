import { Innsatsgruppe } from "mulighetsrommet-api-client";
import { useHentBrukerdata } from "../core/api/queries/useHentBrukerdata";
import { useTiltaksgjennomforingById } from "../core/api/queries/useTiltaksgjennomforingById";

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

function utledInnsatsgrupperFraInnsatsgruppe(innsatsgruppe: string): Innsatsgruppe[] {
  switch (innsatsgruppe) {
    case "STANDARD_INNSATS":
      return [Innsatsgruppe.STANDARD_INNSATS];
    case "SITUASJONSBESTEMT_INNSATS":
      return [Innsatsgruppe.STANDARD_INNSATS, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS];
    case "SPESIELT_TILPASSET_INNSATS":
      return [
        Innsatsgruppe.STANDARD_INNSATS,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      ];
    case "VARIG_TILPASSET_INNSATS":
      return [
        Innsatsgruppe.STANDARD_INNSATS,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
        Innsatsgruppe.VARIG_TILPASSET_INNSATS,
      ];
    default:
      return [];
  }
}
