import { Regelverklenke } from "@api-client";
import { Link, VStack } from "@navikt/ds-react";

interface RegelverkInfoProps {
  regelverkLenker?: Regelverklenke[];
}

interface RegelverklenkeProps {
  regelverkLenke: Regelverklenke;
}

function RegelverklenkeComponent({ regelverkLenke }: RegelverklenkeProps) {
  if (!regelverkLenke.url) {
    return null;
  }

  return (
    <Link target="_blank" href={regelverkLenke.url} key={regelverkLenke.url}>
      {regelverkLenke.navn}{" "}
    </Link>
  );
}

export function RegelverkInfo({ regelverkLenker }: RegelverkInfoProps) {
  return (
    <VStack gap="space-1" align="end">
      {regelverkLenker &&
        regelverkLenker.map((lenke) => (
          <RegelverklenkeComponent key={lenke.url} regelverkLenke={lenke} />
        ))}
    </VStack>
  );
}
