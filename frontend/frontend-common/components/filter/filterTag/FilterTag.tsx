import { Tag } from "@navikt/ds-react";
import { kebabCase } from "../../../utils/TestUtils";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./FilterTag.module.scss";
import Ikonknapp from "../../ikonknapp/Ikonknapp";

interface FiltertagsProps {
  label: string;
  onClose?: () => void;
}

export function FilterTag({ label, onClose }: FiltertagsProps) {
  return (
    <Tag
      size="small"
      variant="info"
      data-testid={`filtertag_${kebabCase(label)}`}
      className={styles.filtertag}
      title={label}
    >
      {label}
      {onClose ? (
        <Ikonknapp
          className={styles.overstyrt_ikon_knapp}
          handleClick={onClose}
          ariaLabel="Lukke"
          data-testid={`filtertag_lukkeknapp_${kebabCase(label)}`}
          icon={
            <XMarkIcon
              data-testid={`filtertag_lukkeknapp_${kebabCase(label)}`}
              aria-label="Lukke"
            />
          }
        />
      ) : null}
    </Tag>
  );
}
