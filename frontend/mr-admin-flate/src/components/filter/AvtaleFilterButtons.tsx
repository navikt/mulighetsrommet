import { useAtom, WritableAtom } from "jotai";
import { AvtaleFilter, defaultAvtaleFilter } from "@/api/atoms";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import style from "./AvtaleFilterButtons.module.scss";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/filter/nullstillFilterKnapp/NullstillFilterKnapp";
import { Lenkeknapp } from "mulighetsrommet-frontend-common/components/lenkeknapp/Lenkeknapp";

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
      filter.arrangorer.length > 0 ? (
        <NullstillFilterKnapp
          onClick={() => {
            setFilter({
              ...defaultAvtaleFilter,
              tiltakstyper: tiltakstypeId ? [tiltakstypeId] : defaultAvtaleFilter.tiltakstyper,
            });
          }}
        />
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
