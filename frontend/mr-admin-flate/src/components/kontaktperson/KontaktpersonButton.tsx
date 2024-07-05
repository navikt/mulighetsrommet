import { Button } from "@navikt/ds-react";
import styles from "./KontaktpersonButton.module.scss";

interface Props {
  onClick: () => void;
  knappetekst: string | React.ReactNode;
}
export function KontaktpersonButton({ onClick, knappetekst }: Props) {
  return (
    <Button
      className={styles.kontaktperson_button}
      size="small"
      type="button"
      variant="tertiary"
      onClick={() => onClick()}
    >
      {knappetekst}
    </Button>
  );
}
