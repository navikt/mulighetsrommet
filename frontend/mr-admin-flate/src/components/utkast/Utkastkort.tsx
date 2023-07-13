import { Utkast } from "mulighetsrommet-api-client";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { DocPencilIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading } from "@navikt/ds-react";
import { z } from "zod";
import { useMineUtkast } from "../../api/utkast/useMineUtkast";
import { formaterDatoTid } from "../../utils/Utils";
import styles from "./Utkastkort.module.scss";
import classNames from "classnames";

interface UtkastKortProps {
  utkast: Utkast;
  onEdit: () => void;
}

const UtkastDataSchema = z.object({
  navn: z.string(),
});

function utkasttypeTekst(type: Utkast.type): "gjennomføring" | "avtale" {
  switch (type) {
    case Utkast.type.AVTALE:
      return "avtale";
    case Utkast.type.TILTAKSGJENNOMFORING:
      return "gjennomføring";
  }
}

export function UtkastKort({ utkast, onEdit }: UtkastKortProps) {
  const slettMutation = useDeleteUtkast();
  const { refetch } = useMineUtkast(utkast.type);

  async function slettUtkast() {
    slettMutation.mutate(utkast.id, { onSuccess: async () => await refetch() });
  }

  const data = UtkastDataSchema.parse(utkast.utkastData);
  return (
    <div className={styles.utkast}>
      <div className={styles.header}>
        <small>Oppdatert: {formaterDatoTid(utkast.updatedAt!)}</small>
        <small className={classNames(styles.tekst_med_ikon, styles.muted)}>
          <DocPencilIcon /> Utkast for {utkasttypeTekst(utkast.type)}
        </small>
      </div>
      <div className={styles.content}>
        <Heading size="medium" className={styles.truncate} title={data?.navn}>
          {data?.navn || "Utkast uten tittel"}
        </Heading>
        <BodyShort className={styles.muted}>
          Opprettet: {formaterDatoTid(utkast.createdAt!)}
        </BodyShort>
      </div>
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
  );
}
