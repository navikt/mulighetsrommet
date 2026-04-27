import { RedaksjoneltInnholdLenke } from "@api-client";
import { Link, VStack } from "@navikt/ds-react";

interface FaglenkerProps {
  faglenker?: RedaksjoneltInnholdLenke[];
}

export function Faglenker({ faglenker }: FaglenkerProps) {
  if (!faglenker) {
    return null;
  }

  return (
    <VStack gap="space-1" align="end">
      {faglenker.map((lenke) => (
        <Faglenke key={lenke.url} lenke={lenke} />
      ))}
    </VStack>
  );
}

interface RegelverklenkeProps {
  lenke: RedaksjoneltInnholdLenke;
}

function Faglenke({ lenke }: RegelverklenkeProps) {
  if (!lenke.url) {
    return null;
  }

  return (
    <Link target="_blank" href={lenke.url} key={lenke.url}>
      {lenke.navn}{" "}
    </Link>
  );
}
