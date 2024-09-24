import { defaultTiltaksgjennomforingfilter, TiltaksgjennomforingFilter } from "@/api/atoms";
import { useAtom } from "jotai/index";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { AvtaleDto, LagretDokumenttype } from "@mr/api-client";
import { WritableAtom } from "jotai";
import { LagreFilterContainer } from "@mr/frontend-common";

interface Props {
  filterAtom: WritableAtom<
    TiltaksgjennomforingFilter,
    [newValue: TiltaksgjennomforingFilter],
    void
  >;
  avtale?: AvtaleDto;
}
export function NullstillKnappForTiltaksgjennomforinger({ filterAtom, avtale }: Props) {
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
            ...defaultTiltaksgjennomforingfilter,
            avtale: avtale?.id ?? defaultTiltaksgjennomforingfilter.avtale,
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
