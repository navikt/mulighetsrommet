import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Link } from "@navikt/ds-react";
import { RegelverkLenke } from "@mr/api-client";
import styles from "./SidemenyInfo.module.scss";

interface RegelverksinfoProps {
  regelverkLenker?: RegelverkLenke[];
}

const Regelverksinfo = ({ regelverkLenker }: RegelverksinfoProps) => {
  const regelverkLenkeComponent = (regelverkLenke: RegelverkLenke) => {
    return (
      regelverkLenke.regelverkUrl && (
        <div key={regelverkLenke._id}>
          <Link target="_blank" href={regelverkLenke.regelverkUrl}>
            {regelverkLenke.regelverkLenkeNavn}{" "}
            <ExternalLinkIcon aria-label="Ikon som representerer at lenke åpnes i ny fane" />
          </Link>
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
