import styles from "./DetaljerInfo.module.scss";
import { Lenkeknapp } from "../components/lenkeknapp/Lenkeknapp";

interface Props {
  redigerIsEnabled: boolean;
  lenke: string;
  lenketekst: string;
}

export function InfoKnapperad({ redigerIsEnabled, lenke, lenketekst }: Props) {
  return (
    <div className={styles.knapperad}>
      {redigerIsEnabled ? (
        <Lenkeknapp
          to={lenke}
          lenketekst={lenketekst}
          variant="primary"
          dataTestId="rediger-avtale"
        />
      ) : null}
    </div>
  );
}
