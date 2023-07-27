import styles from "../DetaljerInfo.module.scss";
import { Button } from "@navikt/ds-react";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";

interface Props {
  handleSlett: () => void;
}

export function AvtaleKnapperad({ handleSlett }: Props) {
  const { data: features } = useFeatureToggles();
  const avtaleId = useGetAvtaleIdFromUrl();

  return (
    <div className={styles.knapperad}>
      {features?.["mulighetsrommet.admin-flate-slett-avtale"] ? (
        <Button
          variant="tertiary-neutral"
          onClick={() => handleSlett()}
          data-testid="slett-avtale"
          className={styles.slett_knapp}
        >
          Feilregistrering
        </Button>
      ) : null}

      {features?.["mulighetsrommet.admin-flate-rediger-avtale"] ? (
        <Lenke to={`/avtaler/skjema?avtaleId=${avtaleId}`}>
          <Button variant="tertiary" data-testid="endre-avtale">
            Endre
          </Button>
        </Lenke>
      ) : null}
    </div>
  );
}
