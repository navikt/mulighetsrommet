import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";
import { UseMutationResult } from "@tanstack/react-query";
import { Utkast } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";

interface Props {
  submit?: boolean;
  onLagreUtkast: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
  dataTestId?: string;
  knappetekst?: string;
}

export function LagreEndringerKnapp({
  submit,
  onLagreUtkast,
  mutationUtkast,
  dataTestId,
  knappetekst,
}: Props) {
  const [mutationIsSuccess, setMutationIsSuccess] = useState(false);

  //måtte sette denne fordi knappen ikke ble enabled på utkast
  useEffect(() => {
    if (mutationUtkast.isSuccess) {
      setMutationIsSuccess(true);
    }
  }, [mutationUtkast]);
  return (
    <Button
      className={styles.button}
      type={submit ? "submit" : "button"}
      variant={submit ? "primary" : "secondary"}
      data-testid={dataTestId}
      onClick={() => {
        faro?.api?.pushEvent(
          "Bruker redigerer avtale",
          { handling: "redigerer" },
          "avtale",
        );
        onLagreUtkast();
      }}
      disabled={!mutationIsSuccess}
    >
      {knappetekst! ? knappetekst : "Lagre endringer"}
    </Button>
  );
}
