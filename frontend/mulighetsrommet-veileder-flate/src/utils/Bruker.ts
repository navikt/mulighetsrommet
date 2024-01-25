import { Bruker } from "mulighetsrommet-api-client";

export function erBrukerReservertMotElektroniskKommunikasjon(brukerdata: Bruker): {
  reservert: boolean;
  melding: string | null;
} {
  if (!brukerdata.manuellStatus) {
    return {
      reservert: true,
      melding:
        "Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert seg mot elektronisk kommunikasjon.",
    };
  } else if (brukerdata.manuellStatus.erUnderManuellOppfolging) {
    return {
      reservert: true,
      melding:
        "Brukeren er under manuell oppfølging og kan derfor ikke benytte seg av våre digitale tjenester.",
    };
  } else if (brukerdata.manuellStatus.krrStatus && brukerdata.manuellStatus.krrStatus.erReservert) {
    return {
      reservert: true,
      melding:
        "Brukeren har reservert seg mot elektronisk kommunikasjon i Kontakt- og reservasjonsregisteret (KRR).",
    };
  } else if (brukerdata.manuellStatus.krrStatus && !brukerdata.manuellStatus.krrStatus.kanVarsles) {
    return {
      reservert: true,
      melding:
        "Brukeren er reservert mot elektronisk kommunikasjon i KRR. Vi kan derfor ikke kommunisere digitalt med denne brukeren.",
    };
  } else {
    return {
      reservert: false,
      melding: null,
    };
  }
}
