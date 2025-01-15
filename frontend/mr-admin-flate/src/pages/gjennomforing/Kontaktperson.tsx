import { GjennomforingKontaktperson } from "@mr/api-client";
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
      {kontaktperson.beskrivelse && <BodyShort>{kontaktperson.beskrivelse}</BodyShort>}
      <BodyShort>{kontaktperson.mobilnummer}</BodyShort>
      <BodyShort className="flex gap-1.5">
        Kontakt via Teams:{" "}
        <Lenke isExternal to={`${TEAMS_DYPLENKE}${kontaktperson.epost}`}>
          {kontaktperson.epost}
        </Lenke>
      </BodyShort>
    </VStack>
  );
}
