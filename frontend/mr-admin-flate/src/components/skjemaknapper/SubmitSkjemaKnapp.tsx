import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";

interface Props {
  type: "avtale" | "gjennomføring";
  mutation: any;
  dataTestId?: string;
  redigeringsmodus: boolean;
}
export function SubmitSkjemaKnapp({
  type,
  mutation,
  dataTestId,
  redigeringsmodus,
}: Props) {
  return (
    <Button
      className={styles.button}
      type="submit"
      onClick={() => {
        faro?.api?.pushEvent(
          `Bruker oppretter ${type}`,
          { handling: "oppretter" },
          type,
        );
      }}
      data-testid={dataTestId}
      disabled={mutation.isLoading}
    >
      {redigeringsmodus
        ? "Lagre endringer"
        : mutation.isLoading
        ? "Lagrer..."
        : `Opprett ${type}`}
    </Button>
  );
}
