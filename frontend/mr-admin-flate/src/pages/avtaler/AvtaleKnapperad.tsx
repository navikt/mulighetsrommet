import styles from "../DetaljerInfo.module.scss";
import { Button } from "@navikt/ds-react";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { Toggles } from "mulighetsrommet-api-client";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";

interface Props {
  handleSlett: () => void;
}

export function AvtaleKnapperad({ handleSlett }: Props) {
  const { data: slettAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_AVTALE,
  );
  const { data: redigerAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_REDIGER_AVTALE,
  );
  const avtaleId = useGetAvtaleIdFromUrl();

  return (
    <div className={styles.knapperad}>
      {slettAvtaleEnabled ? (
        <Button
          variant="tertiary-neutral"
          onClick={() => handleSlett()}
          data-testid="slett-avtale"
          className={styles.slett_knapp}
        >
          Feilregistrering
        </Button>
      ) : null}

      {redigerAvtaleEnabled ? (
        <Lenkeknapp
          to={`/avtaler/skjema?avtaleId=${avtaleId}`}
          lenketekst="Endre"
          variant="tertiary"
          dataTestId="endre-avtale"
        />
      ) : null}
    </div>
  );
}
