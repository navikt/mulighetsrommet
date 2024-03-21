import { Tag } from "@navikt/ds-react";
import { kebabCase } from "../../../utils/TestUtils";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./Filtertag.module.scss";
import Ikonknapp from "../../ikonknapp/Ikonknapp";

interface FiltertagsProps {
  options: { id: string; tittel: string }[];
  onClose?: (id: string) => void;
}

export const Filtertag = ({ options, onClose }: FiltertagsProps) => {
  return (
    <>
      {options.map((filtertype) => {
        return (
          <Tag
            size="small"
            variant="info"
            key={filtertype.id}
            data-testid={`filtertag_${kebabCase(filtertype.tittel)}`}
            className={styles.filtertag}
          >
            {filtertype.tittel}
            {onClose ? (
              <Ikonknapp
                className={styles.overstyrt_ikon_knapp}
                handleClick={() => onClose(filtertype.id)}
                ariaLabel="Lukke"
                data-testid={`filtertag_lukkeknapp_${kebabCase(filtertype.tittel)}`}
                icon={
                  <XMarkIcon
                    data-testid={`filtertag_lukkeknapp_${kebabCase(filtertype.tittel)}`}
                    className={styles.ikon}
                    aria-label="Lukke"
                  />
                }
              />
            ) : null}
          </Tag>
        );
      })}
    </>
  );
};
