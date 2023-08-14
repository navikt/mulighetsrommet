import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";

interface Props {
  type: "avtale" | "gjennomføring";
  mutation: any;
  dataTestId?: string;
}
export function OpprettAvtaleGjennomforingKnapp({
  type,
  mutation,
  dataTestId,
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
      {mutation.isLoading ? "Lagrer..." : `Opprett ${type}`}
    </Button>
  );
}
