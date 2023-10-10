import { PlusIcon } from "@navikt/aksel-icons";
import { HistorikkForBrukerModalInnhold } from "../../components/historikk/HistorikkForBrukerModalInnhold";
import { routes } from "../../routes";
import styles from "./Landingsside.module.scss";
import { Link } from "react-router-dom";

export function Landingsside() {
  return (
    <main className="mulighetsrommet-veileder-flate">
      <div className={styles.container}>
        <div>
          {/**
           * A-tag her istedenfor Link fra react-router-dom pga. merkelig oppførsel fra Aktivitetsplanen og Dialogen.
           */}
          <Link className={styles.cta_link} to={`/${routes.oversikt}`}>
            <PlusIcon color="white" fontSize={30} aria-hidden /> Finn nytt arbeidsmarkedstiltak
          </Link>
        </div>
        <div>
          <h3>Tiltakshistorikk</h3>
          <HistorikkForBrukerModalInnhold />
        </div>
      </div>
    </main>
  );
}
