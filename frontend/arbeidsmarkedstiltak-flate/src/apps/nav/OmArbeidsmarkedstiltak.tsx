import { BodyShort, Heading, Link } from "@navikt/ds-react";
import { PortenLink } from "@/components/PortenLink";

export function OmArbeidsmarkedstiltak() {
  return (
    <div
      style={{
        maxWidth: "100ch",
        margin: "2rem auto",
        backgroundColor: "white",
        padding: "2rem",
        display: "flex",
        gap: "2rem",
      }}
    >
      <div>
        <Heading size="xlarge" style={{ marginBottom: "1rem" }}>
          Om Nav Arbeidsmarkedstiltak
        </Heading>
        <BodyShort>
          I løsningen Nav Arbeidsmarkedstiltak får du oversikt over alle tiltak som veiledere i Nav
          kan benytte seg av i sine Nav-regioner. Du kan bruke filteret på venstresiden til å finne
          tiltak for et lokalkontor eller for én eller flere regioner.
        </BodyShort>
        <BodyShort>
          Denne løsningen utvikles og forvaltes av{" "}
          <Link href="https://teamkatalog.nav.no/team/aa730c95-b437-497b-b1ae-0ccf69a10997">
            Team Valp
          </Link>
          .
        </BodyShort>
        <br />
        <BodyShort>
          Ved spørsmål eller tilbakemeldinger, send oss en melding i <PortenLink />.
        </BodyShort>
      </div>
      <img src={"/teamvalp.png"} style={{ width: "25%", height: "auto" }} />
    </div>
  );
}
