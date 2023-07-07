import { Utkast } from "mulighetsrommet-api-client";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { DocPencilIcon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";
import { z } from "zod";
import { useMineUtkast } from "../../api/utkast/useMineUtkast";
import { formaterDatoTid } from "../../utils/Utils";
import styles from "./Utkastkort.module.scss";

interface UtkastKortProps {
  utkast: Utkast;
  onEdit: () => void;
}

const UtkastDataSchema = z.object({
  navn: z.string(),
});

export function UtkastKort({ utkast, onEdit }: UtkastKortProps) {
  const slettMutation = useDeleteUtkast();
  const { refetch } = useMineUtkast(Utkast.type.TILTAKSGJENNOMFORING);

  async function slettUtkast() {
    slettMutation.mutate(utkast.id, { onSuccess: async () => await refetch() });
  }

  const data = UtkastDataSchema.parse(utkast.utkastData);
  return (
    <div className={styles.utkast}>
      <div>
        <small className={styles.tekst_med_ikon}>
          <DocPencilIcon /> Utkast
        </small>
        <small>Oppdatert: {formaterDatoTid(utkast.updatedAt!)}</small>
        <h2 className={styles.truncate} title={data?.navn}>
          {data?.navn || "Utkast uten tittel"}
        </h2>
      </div>
      <div className={styles.knappe_container}>
        <small>Opprettet: {formaterDatoTid(utkast.createdAt!)}</small>
        <div className={styles.knapper}>
          <Button
            data-testid="rediger-utkast-knapp"
            size="small"
            variant="primary"
            onClick={onEdit}
          >
            Rediger utkast
          </Button>
          <Button
            data-testid="slett-utkast-knapp"
            size="small"
            variant="danger"
            onClick={slettUtkast}
          >
            Slett utkast
          </Button>
        </div>
      </div>
    </div>
  );
}
