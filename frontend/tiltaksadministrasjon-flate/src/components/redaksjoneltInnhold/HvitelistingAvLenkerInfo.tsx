import { BodyShort, InfoCard, Link, List } from "@navikt/ds-react";
import { InformationSquareIcon } from "@navikt/aksel-icons";
import { PORTEN_HVITELISTING_SKJEMA_URL } from "@/constants";

export function HvitelistingAvLenkerInfo() {
  return (
    <InfoCard data-color="info" as="section" aria-label="Informasjon om noe fremhevet">
      <InfoCard.Header icon={<InformationSquareIcon aria-hidden />}>
        <InfoCard.Title>Hvitelisting av eksterne lenker</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content>
        <BodyShort spacing>
          For at ansatt-nettleser skal kunne nå eksterne lenker, må lenkene godkjennes og legges til
          i sikkerhetsfilteret hos Nav.
        </BodyShort>
        <List as="ul">
          <List.Item title="Test først om du får åpnet lenken fra Modia">
            Du kan velge forhåndsvisning fra Handlinger-knappen etter du har lagret.
          </List.Item>
          <List.Item title="Hvis du ikke når nettsiden fra Modia">
            Søk om hvitelisting av lenken via skjemaet{" "}
            <Link target="_blank" href={PORTEN_HVITELISTING_SKJEMA_URL}>
              Bestille åpning av lenke for Fagsystemer/Modia
            </Link>
          </List.Item>
        </List>
      </InfoCard.Content>
    </InfoCard>
  );
}
