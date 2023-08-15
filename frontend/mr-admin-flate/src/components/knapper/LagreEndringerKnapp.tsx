import styles from "../skjema/Skjema.module.scss";
import { Button } from "@navikt/ds-react";
import { faro } from "@grafana/faro-web-sdk";
import { useEffect, useState } from "react";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";

interface Props {
  submit?: boolean;
  onLagreUtkast: () => void;
  dataTestId?: string;
  knappetekst?: string;
}

export function LagreEndringerKnapp({
  submit,
  onLagreUtkast,
  dataTestId,
  knappetekst,
}: Props) {
  const [mutationIsSuccess, setMutationIsSuccess] = useState(false);
  const mutationUtkast = useMutateUtkast();

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
