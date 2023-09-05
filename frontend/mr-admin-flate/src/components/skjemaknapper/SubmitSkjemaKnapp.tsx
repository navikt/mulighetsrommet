import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";

interface Props {
  type: "avtale" | "gjennomføring";
  mutation: any;
  dataTestId?: string;
  redigeringsmodus: boolean;
  utkastmodus?: boolean;
}
export function SubmitSkjemaKnapp({
  type,
  mutation,
  dataTestId,
  redigeringsmodus,
  utkastmodus,
}: Props) {
  return (
    <Button
      className={styles.lagre_knapp}
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
        ? utkastmodus
          ? //TODO hvis utkastet tilhører en aktiv avtale skal det stå "Lagre endringer"
            "Opprett avtale"
          : "Lagre endringer"
        : mutation.isLoading
        ? "Lagrer..."
        : `Opprett ${type}`}
    </Button>
  );
}
