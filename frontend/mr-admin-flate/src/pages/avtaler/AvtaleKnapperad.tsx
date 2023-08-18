import { Toggles } from "mulighetsrommet-api-client";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import styles from "../DetaljerInfo.module.scss";

export function AvtaleKnapperad() {
  const { data: redigerAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_REDIGER_AVTALE,
  );
  const avtaleId = useGetAvtaleIdFromUrl();

  return (
    <div className={styles.knapperad}>
      {/*{slettAvtaleEnabled ? (*/}
      {/*  avtale.avtalestatus === Avtalestatus.AKTIV ? (*/}
      {/*    <Button*/}
      {/*      variant="danger"*/}
      {/*      onClick={handleAvbryt}*/}
      {/*      data-testid="avbryt-avtale"*/}
      {/*    >*/}
      {/*      Avbryt avtale*/}
      {/*    </Button>*/}
      {/*  ) : (*/}
      {/*    <Button*/}
      {/*      variant="tertiary-neutral"*/}
      {/*      onClick={handleSlett}*/}
      {/*      data-testid="slett-avtale"*/}
      {/*      className={styles.slett_knapp}*/}
      {/*    >*/}
      {/*      Feilregistrering*/}
      {/*    </Button>*/}
      {/*  )*/}
      {/*) : null}*/}

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
