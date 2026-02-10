import { RegelverkLenke } from "@api-client";
import { Link, VStack } from "@navikt/ds-react";
interface RegelverkInfoProps {
  regelverkLenker?: RegelverkLenke[];
}

const RegelverkInfo = ({ regelverkLenker }: RegelverkInfoProps) => {
  const regelverkLenkeComponent = (regelverkLenke: RegelverkLenke) => {
    return (
      regelverkLenke.regelverkUrl && (
        <Link target="_blank" href={regelverkLenke.regelverkUrl} key={regelverkLenke._id}>
          {regelverkLenke.regelverkLenkeNavn}{" "}
        </Link>
      )
    );
  };

  return (
    <VStack gap="space-1" align="end">
      {regelverkLenker && regelverkLenker.map(regelverkLenkeComponent)}
    </VStack>
  );
};

export default RegelverkInfo;
