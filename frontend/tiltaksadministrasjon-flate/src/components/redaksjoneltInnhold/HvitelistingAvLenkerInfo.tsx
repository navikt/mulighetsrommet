import { BodyShort, InfoCard, Link } from "@navikt/ds-react";
import { InformationSquareIcon } from "@navikt/aksel-icons";

const TEAM_VALP_TEAMKATALOG_URL =
  "https://teamkatalog.nav.no/team/aa730c95-b437-497b-b1ae-0ccf69a10997";

export function HvitelistingAvLenkerInfo() {
  return (
    <InfoCard data-color="info" as="section" aria-label="Informasjon om noe fremhevet">
      <InfoCard.Header icon={<InformationSquareIcon aria-hidden />}>
        <InfoCard.Title>Hvitelisting av eksterne lenker</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content>
        <BodyShort>
          For at ansatt-nettleser skal kunne nå eksterne lenker, må lenkene godkjennes og legges til
          i sikkerhetsfilteret hos Nav.
        </BodyShort>
        <BodyShort>
          Kontakt{" "}
          <Link target="_blank" href={TEAM_VALP_TEAMKATALOG_URL}>
            Team Valp
          </Link>{" "}
          om lenken(e) ikke fungerer via Modia.
        </BodyShort>
      </InfoCard.Content>
    </InfoCard>
  );
}
