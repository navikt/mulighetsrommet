import { TEAMS_DYPLENKE } from "@mr/frontend-common/constants";
import { GjennomforingKontaktperson } from "@mr/api-client";
import { BodyShort, VStack } from "@navikt/ds-react";
import { ExternalLinkIcon } from "@navikt/aksel-icons";

interface Props {
  kontaktperson: GjennomforingKontaktperson;
}

export function Kontaktperson({ kontaktperson }: Props) {
  return (
    <VStack gap="05">
      <BodyShort>
        <b>{kontaktperson.navn}</b>
      </BodyShort>
      {kontaktperson.beskrivelse && <BodyShort>{kontaktperson.beskrivelse}</BodyShort>}
      <BodyShort>{kontaktperson.mobilnummer}</BodyShort>
      <BodyShort className="flex gap-1.5">
        Kontakt via Teams:{" "}
        <a href={`${TEAMS_DYPLENKE}${kontaktperson.epost}`}>{kontaktperson.epost}</a>{" "}
        <ExternalLinkIcon aria-label="Åpner direktemelding i Teams" />
      </BodyShort>
    </VStack>
  );
}
