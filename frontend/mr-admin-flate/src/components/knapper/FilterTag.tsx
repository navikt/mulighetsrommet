import { Close } from "@navikt/ds-icons";
import { Tag } from "@navikt/ds-react";
import Ikonknapp from "../knapper/Ikonknapp";
import styles from "./Filtertag.module.scss";

interface FilterTagsProps {
  options: { id: string; tittel: string }[];
  handleClick?: (id: string) => void;
  skjulIkon?: boolean;
}

const FilterTag = ({
  options,
  handleClick,
  skjulIkon = false,
}: FilterTagsProps) => {
  return (
    <>
      {options.map((filtertype) => {
        return (
          <Tag variant="info" className="cypress-tag" key={filtertype.id}>
            {filtertype.tittel}
            {skjulIkon ? null : (
              <Ikonknapp
                className={styles.overstyrt_ikon_knapp}
                handleClick={() => handleClick?.(filtertype.id)}
                ariaLabel="Lukke"
                icon={<Close className={styles.ikon} aria-label="Lukke" />}
              />
            )}
          </Tag>
        );
      })}
    </>
  );
};

export default FilterTag;
