import style from "@/pages/avtaler/AvtalerPage.module.scss";
import { AvtaleFilter, defaultAvtaleFilter } from "@/api/atoms";
import { useAtom } from "jotai/index";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { WritableAtom } from "jotai";
import { LagretDokumenttype } from "@mr/api-client";
import { HStack } from "@navikt/ds-react";
import { LagreFilterButton } from "@mr/frontend-common/components/lagreFilter/LagreFilterButton";
import { useLagreFilter } from "@/api/lagret-filter/useLagreFilter";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
  tiltakstypeId?: string;
}

export function NullstillKnappForAvtaler({ filterAtom, tiltakstypeId }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const lagreFilterMutation = useLagreFilter(LagretDokumenttype.AVTALE);

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
          <LagreFilterButton
            onLagre={(r) => {
              lagreFilterMutation.mutate(r);
              lagreFilterMutation.reset();
            }}
            dokumenttype={LagretDokumenttype.AVTALE}
            filter={filter}
          />
        </HStack>
      ) : null}
    </div>
  );
}
