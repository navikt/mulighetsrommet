import { defaultOppgaverFilter, OppgaverFilter } from "@/api/atoms";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { HStack } from "@navikt/ds-react";
import { WritableAtom } from "jotai";
import { useAtom } from "jotai/index";

interface Props {
  filterAtom: WritableAtom<OppgaverFilter, [newValue: OppgaverFilter], void>;
}

export function NullstillKnappForOppgaver({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  return (
    <div className="grid grid-cols-[auto auto] h-[100%] items-center">
      {filter.type.length > 0 || filter.regioner.length > 0 || filter.tiltakstyper.length > 0 ? (
        <HStack gap="2">
          <NullstillFilterKnapp
            onClick={() => {
              setFilter({
                ...defaultOppgaverFilter,
              });
            }}
          />
        </HStack>
      ) : null}
    </div>
  );
}
