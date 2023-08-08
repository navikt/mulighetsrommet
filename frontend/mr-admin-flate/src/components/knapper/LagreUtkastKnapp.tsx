import { Button } from "@navikt/ds-react";
import styles from "../skjema/Skjema.module.scss";
import { UseMutationResult } from "@tanstack/react-query";
import { Utkast } from "mulighetsrommet-api-client";
import { Laster } from "../laster/Laster";

interface Props {
  dataTestId?: string;
  onLagreUtkast: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
}
export function LagreUtkastKnapp({
  dataTestId,
  onLagreUtkast,
  mutationUtkast,
}: Props) {
  return (
    <Button
      className={styles.button}
      type="button"
      variant="secondary"
      data-testid={dataTestId}
      onClick={onLagreUtkast}
      disabled={!mutationUtkast.isSuccess}
    >
      {mutationUtkast.isLoading ? <Laster /> : "Lagre som utkast"}
    </Button>
  );
}
