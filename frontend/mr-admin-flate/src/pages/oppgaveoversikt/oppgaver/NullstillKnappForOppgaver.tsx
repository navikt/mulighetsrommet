import { OppgaverFilterType } from "@/api/atoms";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";

interface Props {
  filter: OppgaverFilterType;
  resetFilter: () => void;
}

export function NullstillKnappForOppgaver({ filter, resetFilter }: Props) {
  return filter.type.length > 0 || filter.regioner.length > 0 || filter.tiltakstyper.length > 0 ? (
    <NullstillFilterKnapp onClick={() => resetFilter()} />
  ) : null;
}
