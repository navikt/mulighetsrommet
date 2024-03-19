import { Button } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { AvtaleFilter, defaultAvtaleFilter } from "../../api/atoms";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import style from "./AvtaleFilterButtons.module.scss";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
  tiltakstypeId?: string;
}

export function AvtaleFilterButtons({ filterAtom, tiltakstypeId }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  return (
    <div className={style.filterbuttons_container}>
      {filter.sok.length > 0 ||
      filter.navRegioner.length > 0 ||
      filter.avtaletyper.length > 0 ||
      (!tiltakstypeId && filter.tiltakstyper.length > 0) ||
      filter.statuser.length > 0 ||
      filter.leverandor.length > 0 ? (
        <Button
          type="button"
          size="small"
          className={style.nullstill_filter}
          variant="tertiary"
          onClick={() => {
            setFilter({
              ...defaultAvtaleFilter,
              tiltakstyper: tiltakstypeId ? [tiltakstypeId] : defaultAvtaleFilter.tiltakstyper,
            });
          }}
        >
          Nullstill filter
        </Button>
      ) : null}
      <HarSkrivetilgang ressurs="Avtale">
        <Lenkeknapp
          to="/avtaler/skjema"
          size="small"
          variant="primary"
          className={style.opprett_avtale_knapp}
        >
          Opprett ny avtale
        </Lenkeknapp>
      </HarSkrivetilgang>
    </div>
  );
}
