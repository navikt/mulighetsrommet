import { RegelverkLenke } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import styles from "./SidemenyInfo.module.scss";

interface RegelverksinfoProps {
  regelverkLenker?: RegelverkLenke[];
}

const Regelverksinfo = ({ regelverkLenker }: RegelverksinfoProps) => {
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

  return (
    <div className={styles.regelverk}>
      {regelverkLenker && regelverkLenker.map(regelverkLenkeComponent)}
    </div>
  );
};

export default Regelverksinfo;
