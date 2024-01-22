import { PlusIcon } from "@navikt/aksel-icons";
import { Link } from "react-router-dom";
import { HistorikkForBrukerModalInnhold } from "../../components/historikk/HistorikkForBrukerModalInnhold";
import styles from "./Landingsside.module.scss";

export function Landingsside() {
  return (
    <main className="mulighetsrommet-veileder-flate">
      <div className={styles.container}>
        <div>
          <Link className={styles.cta_link} to="/arbeidsmarkedstiltak/oversikt">
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
