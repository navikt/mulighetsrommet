import { Button } from "@navikt/ds-react";
import { WritableAtom, useAtom } from "jotai";
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
      {filter.sok.length > 0 ||
      filter.navRegioner.length > 0 ||
      filter.tiltakstyper.length > 0 ||
      filter.statuser.length > 0 ||
      filter.leverandor_orgnr.length > 0 ? (
        <Button
          type="button"
          size="small"
          style={{ maxWidth: "130px" }}
          variant="tertiary"
          onClick={() => {
            setFilter({ ...defaultAvtaleFilter });
          }}
        >
          Nullstill filter
        </Button>
      ) : (
        <div></div>
      )}
      <Lenkeknapp to={`/avtaler/skjema`} size="small" variant="primary">
        Opprett ny avtale
      </Lenkeknapp>
    </div>
  );
}
