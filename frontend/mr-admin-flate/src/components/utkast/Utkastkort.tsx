import { Utkast } from "mulighetsrommet-api-client";
import { DocPencilIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading } from "@navikt/ds-react";
import { z } from "zod";
import { formaterDatoTid } from "../../utils/Utils";
import styles from "./Utkastkort.module.scss";
import classNames from "classnames";
import { useState } from "react";
import SletteModal from "../modal/SletteModal";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import { useMineUtkast } from "../../api/utkast/useMineUtkast";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";

interface UtkastKortProps {
  utkast: Utkast;
}

const UtkastDataSchema = z.object({
  navn: z.string(),
});

export function UtkastKort({ utkast }: UtkastKortProps) {
  const mutation = useDeleteUtkast();
  const [utkastIdForSletting, setUtkastIdForSletting] = useState<null | string>(null);

  const { refetch } = useMineUtkast(utkast.type);

  const utkasttypeTekst = (type: Utkast.type): "gjennomføring" | "avtale" => {
    switch (type) {
      case Utkast.type.AVTALE:
        return "avtale";
      case Utkast.type.TILTAKSGJENNOMFORING:
        return "gjennomføring";
    }
  };

  const data = UtkastDataSchema.parse(utkast.utkastData);

  async function handleDelete() {
    mutation.mutate(utkast.id, {
      onSuccess: async () => {
        setUtkastIdForSletting(null);
        await refetch();
      },
    });
  }

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
          data-testid="slett-utkast-knapp"
          size="small"
          variant="danger"
          onClick={() => setUtkastIdForSletting(utkast.id)}
        >
          Slett utkast
        </Button>
        <Lenkeknapp
          to={
            utkast.type === Utkast.type.AVTALE
              ? `/avtaler/skjema?utkastId=${utkast.id}`
              : `/tiltaksgjennomforinger/skjema?utkastId=${utkast.id}`
          }
          dataTestId="rediger-utkast-knapp"
          variant="primary"
          size="small"
        >
          Rediger utkast
        </Lenkeknapp>
      </div>

      {utkastIdForSletting ? (
        <SletteModal
          modalOpen={!!utkastIdForSletting}
          onClose={() => setUtkastIdForSletting(null)}
          mutation={mutation}
          headerText="Ønsker du å slette utkastet?"
          headerTextError="Kan ikke slette utkastet."
          handleDelete={handleDelete}
        />
      ) : null}
    </div>
  );
}
