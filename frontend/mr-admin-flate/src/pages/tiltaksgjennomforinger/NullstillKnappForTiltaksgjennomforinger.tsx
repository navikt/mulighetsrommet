import { defaultTiltaksgjennomforingfilter, TiltaksgjennomforingFilter } from "@/api/atoms";
import { useAtom } from "jotai/index";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { Avtale, LagretDokumenttype } from "@mr/api-client";
import { WritableAtom } from "jotai";
import { LagreFilterContainer } from "mulighetsrommet-frontend-common";

interface Props {
  filterAtom: WritableAtom<
    TiltaksgjennomforingFilter,
    [newValue: TiltaksgjennomforingFilter],
    void
  >;
  avtale?: Avtale;
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
