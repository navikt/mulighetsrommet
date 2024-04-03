import style from "@/pages/avtaler/AvtalerPage.module.scss";
import { avtaleFilterAtom, defaultAvtaleFilter } from "@/api/atoms";
import { useAtom } from "jotai/index";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/filter/nullstillFilterKnapp/NullstillFilterKnapp";

export const NullstillKnappForAvtaler = () => {
  const [filter, setFilter] = useAtom(avtaleFilterAtom);

  return (
    <div className={style.filterbuttons_container}>
      {filter.sok.length > 0 ||
      filter.navRegioner.length > 0 ||
      filter.avtaletyper.length > 0 ||
      filter.statuser.length > 0 ||
      filter.arrangorer.length > 0 ? (
        <NullstillFilterKnapp
          onClick={() => {
            setFilter({
              ...defaultAvtaleFilter,
              tiltakstyper: defaultAvtaleFilter.tiltakstyper,
            });
          }}
        />
      ) : null}
    </div>
  );
};
