import { RegelverkLenke } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";

interface RegelverkInfoProps {
  regelverkLenker?: RegelverkLenke[];
}

const RegelverkInfo = ({ regelverkLenker }: RegelverkInfoProps) => {
  const regelverkLenkeComponent = (regelverkLenke: RegelverkLenke) => {
    return (
      regelverkLenke.regelverkUrl && (
        <div key={regelverkLenke._id}>
          <Lenke target="_blank" to={regelverkLenke.regelverkUrl}>
            {regelverkLenke.regelverkLenkeNavn}{" "}
          </Lenke>
        </div>
      )
    );
  };

  return <div>{regelverkLenker && regelverkLenker.map(regelverkLenkeComponent)}</div>;
};

export default RegelverkInfo;
