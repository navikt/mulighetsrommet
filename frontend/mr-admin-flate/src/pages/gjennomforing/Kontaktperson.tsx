import { GjennomforingKontaktperson } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { TEAMS_DYPLENKE } from "@mr/frontend-common/constants";
import { BodyShort, VStack } from "@navikt/ds-react";

interface Props {
  kontaktperson: GjennomforingKontaktperson;
}

export function Kontaktperson({ kontaktperson }: Props) {
  return (
    <VStack gap="05">
      <BodyShort>
        <b>{kontaktperson.navn}</b>
      </BodyShort>
      {kontaktperson.beskrivelse && (
        <BodyShort size="small" className="italic">
          {kontaktperson.beskrivelse}
        </BodyShort>
      )}
      <BodyShort>{kontaktperson.mobilnummer}</BodyShort>
      <BodyShort size="small" className="flex gap-1.5">
        Teamslenke:{" "}
        <Lenke isExternal to={`${TEAMS_DYPLENKE}${kontaktperson.epost}`}>
          {kontaktperson.epost}
        </Lenke>
      </BodyShort>
    </VStack>
  );
}
