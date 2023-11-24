import { Avtale } from "mulighetsrommet-api-client";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import styles from "../DetaljerInfo.module.scss";

interface Props {
  avtale: Avtale;
}

export function AvtaleKnapperad({ avtale }: Props) {
  return (
    <div className={styles.knapperad}>
      <Lenkeknapp size="small" to={`/avtaler/${avtale.id}/skjema`} variant="primary">
        Rediger avtale
      </Lenkeknapp>
    </div>
  );
}
