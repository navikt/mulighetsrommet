import { Utkast } from "mulighetsrommet-api-client";
import { DocPencilIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Heading } from "@navikt/ds-react";
import { z } from "zod";
import { formaterDatoTid } from "../../utils/Utils";
import styles from "./Utkastkort.module.scss";
import classNames from "classnames";
import { useState } from "react";
import { UseMutationResult } from "@tanstack/react-query";
import SletteModal from "../modal/SletteModal";
import invariant from "tiny-invariant";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";

interface UtkastKortProps {
  utkast: Utkast;
  mutation: UseMutationResult<string, unknown, string>;
}

const UtkastDataSchema = z.object({
  navn: z.string(),
});

export function UtkastKort({ utkast, mutation }: UtkastKortProps) {
  const [utkastIdForSletting, setUtkastIdForSletting] = useState<null | string>(
    null,
  );

  const utkasttypeTekst = (type: Utkast.type): "gjennomføring" | "avtale" => {
    switch (type) {
      case Utkast.type.AVTALE:
        return "avtale";
      case Utkast.type.TILTAKSGJENNOMFORING:
        return "gjennomføring";
    }
  };

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
        <Lenkeknapp
          to={`/avtaler/skjema?avtaleId=${utkast.avtaleId}`}
          lenketekst="Rediger utkast"
          dataTestId="rediger-utkast-knapp"
          variant="primary"
          size="small"
        />
        <Button
          data-testid="slett-utkast-knapp"
          size="small"
          variant="danger"
          onClick={() => setUtkastIdForSletting(utkast.id)}
        >
          Slett utkast
        </Button>
      </div>

      {utkastIdForSletting ? (
        <SletteModal
          modalOpen={!!utkastIdForSletting}
          onClose={() => setUtkastIdForSletting(null)}
          mutation={mutation}
          headerText="Ønsker du å slette utkastet?"
          headerTextError="Kan ikke slette utkastet."
          handleDelete={() =>
            mutation.mutate(utkast.id, {
              onSuccess: () => setUtkastIdForSletting(null),
            })
          }
          invariantFunksjon={() =>
            invariant(utkastIdForSletting, "Fant ikke id for å slette utkast.")
          }
        />
      ) : null}
    </div>
  );
}
