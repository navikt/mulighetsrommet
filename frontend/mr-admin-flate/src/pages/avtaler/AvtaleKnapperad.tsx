import styles from "../DetaljerInfo.module.scss";
import { Button } from "@navikt/ds-react";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import { Toggles } from "mulighetsrommet-api-client";

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
        <Lenke to={`/avtaler/skjema?avtaleId=${avtaleId}`}>
          <Button variant="tertiary" data-testid="endre-avtale">
            Endre
          </Button>
        </Lenke>
      ) : null}
    </div>
  );
}
