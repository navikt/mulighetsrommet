import { RedaksjoneltInnholdLenke } from "@api-client";
import { Link, VStack } from "@navikt/ds-react";

interface RegelverkInfoProps {
  regelverkLenker?: RedaksjoneltInnholdLenke[];
}

export function RegelverkInfo({ regelverkLenker }: RegelverkInfoProps) {
  if (!regelverkLenker) {
    return null;
  }

  return (
    <VStack gap="space-1" align="end">
      {regelverkLenker.map((lenke) => (
        <Regelverklenke key={lenke.url} lenke={lenke} />
      ))}
    </VStack>
  );
}

interface RegelverklenkeProps {
  lenke: RedaksjoneltInnholdLenke;
}

function Regelverklenke({ lenke }: RegelverklenkeProps) {
  if (!lenke.url) {
    return null;
  }

  return (
    <Link target="_blank" href={lenke.url} key={lenke.url}>
      {lenke.navn}{" "}
    </Link>
  );
}
