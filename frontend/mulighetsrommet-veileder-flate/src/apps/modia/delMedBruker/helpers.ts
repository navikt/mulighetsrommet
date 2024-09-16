import { ArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { Bruker, NavEnhet, VeilederflateTiltak } from "@mr/api-client";

export function brukersEnhetFilterHasChanged(
  filter: ArbeidsmarkedstiltakFilter,
  bruker: Bruker,
): boolean {
  if (filter.navEnheter.length !== bruker.enheter.length) return true;

  return (
    bruker.enheter
      .map((enhet: NavEnhet) => enhet.enhetsnummer)
      .sort()
      .join(",") !== filter.navEnheter.sort().join(",")
  );
}

export function utledDelMedBrukerTekst(tiltak: VeilederflateTiltak, veiledernavn?: string) {
  const deletekst = getDelMedBrukerTekst(tiltak) ?? "";
  const templatedDeletekst = deletekst.replaceAll("<tiltaksnavn>", tiltak.tittel);
  const hilsen = hilsenTekst(veiledernavn);
  return `Hei\n\n${templatedDeletekst}\n\n${hilsen}`;
}

function hilsenTekst(veiledernavn?: string) {
  const interessant = "Er dette aktuelt for deg? Gi meg tilbakemelding her i dialogen.";
  return veiledernavn ? `${interessant}\n\nHilsen ${veiledernavn}` : `${interessant}`;
}

export function getDelMedBrukerTekst(tiltak: VeilederflateTiltak): string | undefined {
  return tiltak.faneinnhold?.delMedBruker ?? tiltak.tiltakstype.delingMedBruker;
}

export function erBrukerReservertMotDigitalKommunikasjon(brukerdata: Bruker): {
  reservert: boolean;
  melding: string | null;
} {
  if (!brukerdata.manuellStatus) {
    return {
      reservert: true,
      melding:
        "Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert seg mot digital kommunikasjon.",
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
        "Brukeren har reservert seg mot digital kommunikasjon i Kontakt- og reservasjonsregisteret (KRR).",
    };
  } else if (brukerdata.manuellStatus.krrStatus && !brukerdata.manuellStatus.krrStatus.kanVarsles) {
    return {
      reservert: true,
      melding: "Brukeren er reservert mot digital kommunikasjon i KRR.",
    };
  } else {
    return {
      reservert: false,
      melding: null,
    };
  }
}
