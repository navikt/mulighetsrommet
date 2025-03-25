import { Tag } from "@navikt/ds-react";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./FilterTag.module.scss";
import Ikonknapp from "../../ikonknapp/Ikonknapp";

interface FiltertagsProps {
  label: string;
  testId?: string;
  onClose?: () => void;
}

export function FilterTag({ label, testId, onClose }: FiltertagsProps) {
  const actualTestId = testId ?? label;
  return (
    <Tag
      size="small"
      variant="info"
      data-testid={`filtertag_${actualTestId}`}
      className={styles.filtertag}
      title={label}
    >
      {label}
      {onClose ? (
        <Ikonknapp
          className={styles.overstyrt_ikon_knapp}
          handleClick={onClose}
          ariaLabel="Lukke"
          data-testid={`filtertag_lukkeknapp_${actualTestId}`}
          icon={
            <XMarkIcon data-testid={`filtertag_lukkeknapp_${actualTestId}`} aria-label="Lukke" />
          }
        />
      ) : null}
    </Tag>
  );
}
