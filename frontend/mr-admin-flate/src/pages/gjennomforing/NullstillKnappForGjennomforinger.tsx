import { defaultGjennomforingfilter, GjennomforingFilter } from "@/api/atoms";
import { AvtaleDto, LagretDokumenttype } from "@mr/api-client-v2";
import { LagreFilterButton } from "@mr/frontend-common/components/lagreFilter/LagreFilterButton";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { WritableAtom } from "jotai";
import { useAtom } from "jotai/index";
import { useFetcher } from "react-router";
import { filterToActionRequest } from "@/api/lagret-filter/lagretFilterAction";

interface Props {
  filterAtom: WritableAtom<GjennomforingFilter, [newValue: GjennomforingFilter], void>;
  avtale?: AvtaleDto;
}
export function NullstillKnappForGjennomforinger({ filterAtom, avtale }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const fetcher = useFetcher();

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
      <LagreFilterButton
        filter={filter}
        onLagre={(r) => {
          const formData = filterToActionRequest(r, LagretDokumenttype.GJENNOMFORING);
          fetcher.submit(formData, {
            method: "POST",
            action: "/gjennomforinger",
          });
        }}
      />
    </>
  ) : null;
}
