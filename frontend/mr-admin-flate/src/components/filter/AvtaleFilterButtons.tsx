import { Button } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { shallowEquals } from "mulighetsrommet-frontend-common";
import { AvtaleFilter, defaultAvtaleFilter } from "../../api/atoms";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
}

export function AvtaleFilterButtons({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "row",
        justifyContent: "space-between",
        height: "100%",
        alignItems: "center",
      }}
    >
      {!shallowEquals(filter, defaultAvtaleFilter) ? (
        <Button
          type="button"
          size="small"
          style={{ maxWidth: "130px" }}
          variant="tertiary"
          onClick={() => {
            setFilter(defaultAvtaleFilter);
          }}
        >
          Nullstill filter
        </Button>
      ) : null}
      <Lenkeknapp to="/avtaler/skjema" size="small" variant="primary">
        Opprett ny avtale
      </Lenkeknapp>
    </div>
  );
}
