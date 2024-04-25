import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Link } from "@navikt/ds-react";
import { SanityRegelverkLenke } from "mulighetsrommet-api-client";
import styles from "./Sidemenydetaljer.module.scss";

interface RegelverksinfoProps {
  regelverkLenker?: SanityRegelverkLenke[];
}

const Regelverksinfo = ({ regelverkLenker }: RegelverksinfoProps) => {
  const regelverkLenkeComponent = (regelverkLenke: SanityRegelverkLenke) => {
    return (
      regelverkLenke.regelverkUrl && (
        <div key={regelverkLenke._id}>
          <Link target="_blank" href={regelverkLenke.regelverkUrl}>
            {regelverkLenke.regelverkLenkeNavn} <ExternalLinkIcon />
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
