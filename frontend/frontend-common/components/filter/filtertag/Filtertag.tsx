import { Tag } from "@navikt/ds-react";
import { kebabCase } from "../../../utils/TestUtils";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./Filtertag.module.scss";
import Ikonknapp from "../../ikonknapp/Ikonknapp";

interface FiltertagsProps {
  label: string;
  onClose?: () => void;
}

export const Filtertag = ({ label, onClose }: FiltertagsProps) => {
  return (
    <Tag
      size="small"
      variant="info"
      data-testid={`filtertag_${kebabCase(label)}`}
      className={styles.filtertag}
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
              className={styles.ikon}
              aria-label="Lukke"
            />
          }
        />
      ) : null}
    </Tag>
  );
};
