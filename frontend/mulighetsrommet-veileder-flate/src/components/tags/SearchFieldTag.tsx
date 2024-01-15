import { XMarkIcon } from "@navikt/aksel-icons";
import { Tag } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  defaultTiltaksgjennomforingfilter,
  tiltaksgjennomforingsfilter,
} from "../../core/atoms/atoms";
import Ikonknapp from "../knapper/Ikonknapp";
import style from "./Filtertag.module.scss";

const SearchFieldTag = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  const handleClickFjernFilter = () => {
    setFilter(defaultTiltaksgjennomforingfilter);
  };

  return (
    <>
      {filter.search && (
        <Tag variant="info" size="small">
          {`'${filter.search}'`}
          <Ikonknapp
            handleClick={handleClickFjernFilter}
            ariaLabel="Lukkeknapp"
            icon={<XMarkIcon className={style.ikon} aria-label="Lukkeknapp" />}
          />
        </Tag>
      )}
    </>
  );
};

export default SearchFieldTag;
