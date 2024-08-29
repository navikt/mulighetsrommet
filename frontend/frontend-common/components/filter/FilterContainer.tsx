import { XMarkIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, HStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import styles from "./FilterContainer.module.scss";

interface Props {
  title: "Filter" | "Lagrede filter";
  children: ReactNode;
  onClose: () => void;
}

export function FilterContainer({ title, children, onClose }: Props) {
  return (
    <>
      <HStack className={styles.container} align={"center"} justify={"space-between"}>
        <BodyShort className={styles.bold}>{title}</BodyShort>
        <Button className={styles.button} variant="tertiary-neutral" onClick={onClose}>
          <XMarkIcon aria-label="Kryss for å lukke filter" />
        </Button>
      </HStack>

      {children}
    </>
  );
}
