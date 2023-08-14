import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import styles from "../DetaljerInfo.module.scss";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";

export function AvtaleKnapperad() {
  const { data: redigerAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_REDIGER_AVTALE,
  );

  const avtaleId = useGetAvtaleIdFromUrl();

  return (
    <div className={styles.knapperad}>
      {redigerAvtaleEnabled ? (
        <Lenkeknapp
          to={`/avtaler/skjema?avtaleId=${avtaleId}`}
          lenketekst="Rediger avtale"
          variant="primary"
          dataTestId="endre-avtale"
        />
      ) : null}
    </div>
  );
}
