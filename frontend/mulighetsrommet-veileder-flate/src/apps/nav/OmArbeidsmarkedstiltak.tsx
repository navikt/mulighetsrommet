import { Link } from "@navikt/ds-react";

export function OmArbeidsmarkedstiltak() {
  return (
    <div style={{ maxWidth: "65ch" }}>
      <h1>Om NAV Arbeidsmarkedstiltak</h1>
      <p>
        I løsningen NAV Arbeidsmarkedstiltak får du oversikt over alle tiltak som veiledere i NAV
        kan benytte seg av i sine NAV-regioner. Du kan bruke filteret på venstresiden til å finne
        tiltak for et lokalkontor eller for én eller flere regioner.
      </p>
      <p>
        Denne løsningen utvikles og forvaltes av{" "}
        <Link href="https://teamkatalog.nav.no/team/aa730c95-b437-497b-b1ae-0ccf69a10997">
          Team Valp
        </Link>
        .
      </p>
      <p>
        Har du spørsmål eller innspill kan du{" "}
        <Link href="https://slack.com/app_redirect?team=T5LNAMWNA&channel=team-valp">
          sende oss en melding på Slack i kanalen #team-valp
        </Link>
        .
      </p>
    </div>
  );
}
