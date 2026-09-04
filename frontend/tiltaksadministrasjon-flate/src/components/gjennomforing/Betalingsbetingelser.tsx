import { Heading } from "@navikt/ds-react";
import { PrismodellDto } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { PrismodellDetaljer } from "@/components/prismodell/PrismodellDetaljer";

interface Props {
  prismodell: PrismodellDto;
}

export function Betalingsbetingelser({ prismodell }: Props) {
  return (
    <>
      <Heading level="3" size="small" spacing>
        {avtaletekster.prismodell.heading}
      </Heading>
      <PrismodellDetaljer prismodell={prismodell} />
    </>
  );
}
