import { AvtaleFilterType } from "@/api/atoms";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";

interface Props {
  filter: AvtaleFilterType;
  resetFilter: () => void;
}

export function NullstillKnappForAvtaler({ filter, resetFilter }: Props) {
  return filter.visMineAvtaler ||
    filter.sok.length > 0 ||
    filter.navRegioner.length > 0 ||
    filter.avtaletyper.length > 0 ||
    filter.statuser.length > 0 ||
    filter.personvernBekreftet !== undefined ||
    filter.arrangorer.length > 0 ? (
    <NullstillFilterKnapp onClick={resetFilter} />
  ) : null;
}
