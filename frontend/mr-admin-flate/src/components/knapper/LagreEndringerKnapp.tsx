import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";
import { UseMutationResult } from "@tanstack/react-query";
import { Utkast } from "mulighetsrommet-api-client";

interface Props {
  submit?: boolean;
  onLagreUtkast: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}

export function LagreEndringerKnapp({
  submit = true,
  onLagreUtkast,
  mutationUtkast,
}: Props) {
  return (
    <Button
      className={styles.button}
      type={submit ? "submit" : "button"}
      variant={submit ? "primary" : "secondary"}
      onClick={() => {
        faro?.api?.pushEvent(
          "Bruker redigerer avtale",
          { handling: "redigerer" },
          "avtale",
        );
        onLagreUtkast();
      }}
      disabled={!mutationUtkast.isSuccess}
    >
      Lagre endringer
    </Button>
  );
}
