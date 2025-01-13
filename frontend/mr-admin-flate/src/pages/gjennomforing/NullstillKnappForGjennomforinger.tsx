import { defaultGjennomforingfilter, GjennomforingFilter } from "@/api/atoms";
import { useAtom } from "jotai/index";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { AvtaleDto, LagretDokumenttype } from "@mr/api-client";
import { WritableAtom } from "jotai";
import { LagreFilterContainer } from "@mr/frontend-common";

interface Props {
  filterAtom: WritableAtom<GjennomforingFilter, [newValue: GjennomforingFilter], void>;
  avtale?: AvtaleDto;
}
export function NullstillKnappForGjennomforinger({ filterAtom, avtale }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  return filter.visMineGjennomforinger ||
    filter.search.length > 0 ||
    filter.tiltakstyper.length > 0 ||
    filter.navEnheter.length > 0 ||
    filter.statuser.length > 0 ||
    filter.arrangorer.length > 0 ? (
    <>
      <NullstillFilterKnapp
        onClick={() => {
          setFilter({
            ...defaultGjennomforingfilter,
            avtale: avtale?.id ?? defaultGjennomforingfilter.avtale,
          });
        }}
      />
      <LagreFilterContainer
        dokumenttype={LagretDokumenttype.TILTAKSGJENNOMFØRING}
        filter={filter}
      />
    </>
  ) : null;
}
