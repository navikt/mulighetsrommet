import { OppgaverFilterType } from "@/api/atoms";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { HStack } from "@navikt/ds-react";

interface Props {
  filter: OppgaverFilterType;
  resetFilter: () => void;
}

export function NullstillKnappForOppgaver({ filter, resetFilter }: Props) {
  return (
    <div className="grid grid-cols-[auto auto] h-[100%] items-center">
      {filter.type.length > 0 || filter.regioner.length > 0 || filter.tiltakstyper.length > 0 ? (
        <HStack gap="2">
          <NullstillFilterKnapp onClick={() => resetFilter()} />
        </HStack>
      ) : null}
    </div>
  );
}
