import { Regelverklenke } from "@api-client";
import { Link, VStack } from "@navikt/ds-react";

interface RegelverkInfoProps {
  regelverkLenker?: Regelverklenke[];
}

interface RegelverklenkeProps {
  regelverkLenke: Regelverklenke;
}

function RegelverklenkeComponent({ regelverkLenke }: RegelverklenkeProps) {
  if (!regelverkLenke.regelverkUrl) {
    return null;
  }

  return (
    <Link target="_blank" href={regelverkLenke.regelverkUrl} key={regelverkLenke.regelverkUrl}>
      {regelverkLenke.regelverkLenkeNavn}{" "}
    </Link>
  );
}

export function RegelverkInfo({ regelverkLenker }: RegelverkInfoProps) {
  return (
    <VStack gap="space-1" align="end">
      {regelverkLenker &&
        regelverkLenker.map((lenke) => (
          <RegelverklenkeComponent key={lenke.regelverkUrl} regelverkLenke={lenke} />
        ))}
    </VStack>
  );
}
