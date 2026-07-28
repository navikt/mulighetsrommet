import { BodyShort, Box, CopyButton, HGrid, InfoCard, Link } from "@navikt/ds-react";
import { InformationSquareIcon } from "@navikt/aksel-icons";

const PORTEN_MELDE_HVITELISTING_URL =
  "https://jira.adeo.no/plugins/servlet/desk/portal/542/create/2441?summary=Bestille åpning av lenke for Fagsystemportalen%2FModia";

const PORTEN_SKJEMA_VERDIER = [
  {
    field: "Tjeneste",
    value: "Nettverk - Ukategorisert",
  },
  {
    field: "Ansvarlig gruppe",
    value: "Team Secops Services",
  },
];

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
          Endringsønsket{" "}
          <Link target="_blank" href={PORTEN_MELDE_HVITELISTING_URL}>
            meldes inn via Porten
          </Link>{" "}
          med følgende:
          <Box paddingBlock="space-4">
            <HGrid width="50%" columns="1fr auto auto" gap="space-4" align="center">
              {PORTEN_SKJEMA_VERDIER.map(({ field, value }) => (
                <>
                  <b>{field}:</b>
                  <span>{value}</span>
                  <CopyButton size="small" copyText={value} />
                </>
              ))}
            </HGrid>
          </Box>
        </BodyShort>
      </InfoCard.Content>
    </InfoCard>
  );
}
