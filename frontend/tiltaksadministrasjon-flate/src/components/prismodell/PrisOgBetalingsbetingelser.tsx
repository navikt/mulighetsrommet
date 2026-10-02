import { Heading, HeadingProps } from "@navikt/ds-react";
import { PrismodellDto } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { PrismodellDetaljer } from "@/components/prismodell/PrismodellDetaljer";

interface Props {
  prismodell: PrismodellDto;
  size?: HeadingProps["size"];
}

export function PrisOgBetalingsbetingelser({ prismodell, size = "small" }: Props) {
  return (
    <>
      <Heading level="3" size={size} spacing>
        {avtaletekster.prismodell.heading}
      </Heading>
      <PrismodellDetaljer prismodell={prismodell} />
    </>
  );
}
