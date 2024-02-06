import {
  ArbeidsmarkedstiltakFilter,
  valgteEnhetsnumre,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { Bruker, NavEnhet, VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";

export function brukersEnhetFilterHasChanged(
  filter: ArbeidsmarkedstiltakFilter,
  bruker?: Bruker,
): boolean {
  if (!bruker) return false;

  const filterEnheter = valgteEnhetsnumre(filter);
  if (filterEnheter.length !== bruker.enheter.length) return true;

  return (
    bruker.enheter
      .map((enhet: NavEnhet) => enhet.enhetsnummer)
      .sort()
      .join(",") !== filterEnheter.sort().join(",")
  );
}

export function utledDelMedBrukerTekst(
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing,
  veiledernavn?: string,
) {
  const deletekst = getDelMedBrukerTekst(tiltaksgjennomforing) ?? "";
  const tiltak = deletekst.replaceAll("<tiltaksnavn>", tiltaksgjennomforing.navn);

  const hilsen = hilsenTekst(veiledernavn);
  return `Hei\n\n${tiltak}\n\n${hilsen}`;
}

function hilsenTekst(veiledernavn?: string) {
  const interessant = "Er dette aktuelt for deg? Gi meg tilbakemelding her i dialogen.";
  return veiledernavn ? `${interessant}\n\nHilsen ${veiledernavn}` : `${interessant}`;
}

export function getDelMedBrukerTekst(
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing,
): string | undefined {
  return (
    tiltaksgjennomforing.faneinnhold?.delMedBruker ??
    tiltaksgjennomforing.tiltakstype.delingMedBruker
  );
}

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
