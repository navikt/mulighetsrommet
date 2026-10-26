import { BodyShort, HStack, Tooltip, VStack } from "@navikt/ds-react";
import { Gradering } from "@tiltaksadministrasjon/api-client";
import { graderingVisning } from "./graderingVisning";

interface Props {
  navn: string;
  gradering: Gradering;
  norskIdent: string | null;
}

export function NavnOgGradering({ navn, gradering, norskIdent }: Props) {
  function graderingIkon() {
    const visning = graderingVisning(gradering);
    if (!visning) {
      return null;
    }
    const { Ikon, label, color } = visning;
    return (
      <Tooltip content={label}>
        <Ikon color={`var(--ax-text-${color}-decoration)`} fontSize="1.25rem" />
      </Tooltip>
    );
  }

  return (
    <VStack>
      <HStack align="center" gap="space-8" wrap={false}>
        <BodyShort weight="semibold">{navn}</BodyShort>
        {graderingIkon()}
      </HStack>
      {norskIdent && <BodyShort>{norskIdent}</BodyShort>}
    </VStack>
  );
}
