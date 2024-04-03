import { defaultTiltaksgjennomforingfilter, tiltaksgjennomforingfilterAtom } from "@/api/atoms";
import { useAtom } from "jotai/index";
import { NullstillFilterKnapp } from "mulighetsrommet-frontend-common/components/filter/nullstillFilterKnapp/NullstillFilterKnapp";
import { Avtale } from "mulighetsrommet-api-client";

interface Props {
  avtale?: Avtale;
}
export const NullstillKnappForTiltaksgjennomforinger = ({ avtale }: Props) => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilterAtom);

  return filter.search.length > 0 ||
    filter.tiltakstyper.length > 0 ||
    filter.navEnheter.length > 0 ||
    filter.statuser.length > 0 ||
    filter.arrangorer.length > 0 ? (
    <NullstillFilterKnapp
      onClick={() => {
        setFilter({
          ...defaultTiltaksgjennomforingfilter,
          avtale: avtale?.id ?? defaultTiltaksgjennomforingfilter.avtale,
        });
      }}
    />
  ) : null;
};
