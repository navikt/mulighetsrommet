import { XMarkIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, HStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./FilterContainer.module.scss";

interface Props {
  children: ReactNode;
  onClose: () => void;
}

export function FilterContainer({ children, onClose }: Props) {
  return (
    <>
      <HStack className={styles.container} align={"center"} justify={"space-between"}>
        <BodyShort className={styles.bold}>Filter</BodyShort>
        <Button className={styles.button} variant="tertiary-neutral" onClick={onClose}>
          <XMarkIcon aria-label="Kryss for å lukke filter" />
        </Button>
      </HStack>

      {children}
    </>
  );
}
