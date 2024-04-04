import style from "@/pages/avtaler/AvtalerPage.module.scss";
import { AvtaleFilter, defaultAvtaleFilter } from "@/api/atoms";
import { useAtom } from "jotai/index";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/filter/nullstillFilterKnapp/NullstillFilterKnapp";
import { WritableAtom } from "jotai";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
  tiltakstypeId?: string;
}

export const NullstillKnappForAvtaler = ({ filterAtom, tiltakstypeId }: Props) => {
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
    </div>
  );
};
