import style from "@/pages/avtaler/AvtalerPage.module.scss";
import { AvtaleFilter, defaultAvtaleFilter } from "@/api/atoms";
import { useAtom } from "jotai/index";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { WritableAtom } from "jotai";
import { LagreFilterKnapp } from "../../components/lagretFilter/LagreFilterKnapp";
import { LagretDokumenttype } from "mulighetsrommet-api-client";
import { HStack } from "@navikt/ds-react";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
  tiltakstypeId?: string;
}

export const NullstillKnappForAvtaler = ({ filterAtom, tiltakstypeId }: Props) => {
  const [filter, setFilter] = useAtom(filterAtom);

  return (
    <div className={style.filterbuttons_container}>
      {filter.visMineAvtaler ||
      filter.sok.length > 0 ||
      filter.navRegioner.length > 0 ||
      filter.avtaletyper.length > 0 ||
      (!tiltakstypeId && filter.tiltakstyper.length > 0) ||
      filter.statuser.length > 0 ||
      filter.personvernBekreftet.length > 0 ||
      filter.arrangorer.length > 0 ? (
        <HStack gap="2">
          <NullstillFilterKnapp
            onClick={() => {
              setFilter({
                ...defaultAvtaleFilter,
                tiltakstyper: tiltakstypeId ? [tiltakstypeId] : defaultAvtaleFilter.tiltakstyper,
              });
            }}
          />
          <LagreFilterKnapp dokumenttype={LagretDokumenttype.AVTALE} />
        </HStack>
      ) : null}
    </div>
  );
};
