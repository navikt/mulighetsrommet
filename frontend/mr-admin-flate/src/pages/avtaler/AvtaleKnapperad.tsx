import { Button } from "@navikt/ds-react";
import { Avtale, Avtalestatus, Toggles } from "mulighetsrommet-api-client";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import styles from "../DetaljerInfo.module.scss";

interface Props {
  handleSlett: () => void;
  avtale: Avtale;
}

export function AvtaleKnapperad({ handleSlett, avtale }: Props) {
  const { data: slettAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_AVTALE,
  );
  const avtaleId = useGetAvtaleIdFromUrl();

  return (
    <div className={styles.knapperad}>
      {slettAvtaleEnabled &&
        [Avtalestatus.PLANLAGT, Avtalestatus.AKTIV].includes(avtale.avtalestatus) && (
          <Button
            size="small"
            variant="tertiary-neutral"
            onClick={handleSlett}
            className={styles.slett_knapp}
          >
            Feilregistrering
          </Button>
        )}
      <Lenkeknapp size="small" to={`/avtaler/${avtaleId}/skjema`} variant="primary">
        Rediger avtale
      </Lenkeknapp>
    </div>
  );
}
