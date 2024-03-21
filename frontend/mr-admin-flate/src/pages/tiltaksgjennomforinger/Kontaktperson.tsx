import { BodyShort, VStack } from "@navikt/ds-react";
import { TiltaksgjennomforingKontaktperson } from "mulighetsrommet-api-client";
import { TEAMS_DYPLENKE } from "mulighetsrommet-frontend-common/constants";
import { TeamsIkon } from "../../components/ikoner/TeamsIkon";

interface Props {
  kontaktperson: TiltaksgjennomforingKontaktperson;
}

export function Kontaktperson({ kontaktperson }: Props) {
  return (
    <VStack gap="05">
      <BodyShort>
        <b>{kontaktperson.navn}</b>
      </BodyShort>
      {kontaktperson.beskrivelse && <BodyShort>{kontaktperson.beskrivelse}</BodyShort>}
      <BodyShort>{kontaktperson.mobilnummer}</BodyShort>
      <BodyShort>
        Kontakt via Teams:{" "}
        <a href={`${TEAMS_DYPLENKE}${kontaktperson.epost}`}>{kontaktperson.epost}</a>{" "}
        <TeamsIkon aria-label="Åpner direktemelding i Teams" />
      </BodyShort>
    </VStack>
  );
}
