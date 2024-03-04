import { Button } from "@navikt/ds-react";
import { LayersPlusIcon } from "@navikt/aksel-icons";
import styles from "./DupliserButton.module.scss";

interface Props {
  onClick: () => void;
  title: string;
}

export function DupliserButton({ onClick, title }: Props) {
  return (
    <Button title={title} className={styles.button} onClick={onClick}>
      <div className={styles.button_inner}>
        <LayersPlusIcon
          color="white"
          fontSize="1.5rem"
          aria-label="Ikon for duplisering av dokument"
        />
        Dupliser
      </div>
    </Button>
  );
}
