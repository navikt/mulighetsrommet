import { Heading } from "@navikt/ds-react";
import { PrismodellDetaljer } from "@/components/avtaler/PrismodellDetaljer";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { PrismodellDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  prismodell: PrismodellDto;
}

export function GjennomforingPrismodellDetaljer({ prismodell }: Props) {
  return (
    <>
      <Heading level="3" size="small" spacing>
        {avtaletekster.prismodell.heading}
      </Heading>
      <PrismodellDetaljer prismodell={[prismodell]} />
    </>
  );
}
