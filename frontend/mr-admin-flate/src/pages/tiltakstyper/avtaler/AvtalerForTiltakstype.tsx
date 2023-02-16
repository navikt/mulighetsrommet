import { Avtalefilter } from "../../../components/avtaler/Avtalefilter";
import { AvtaleTabell } from "./AvtaleTabell";

export function AvtalerForTiltakstype() {
  return (
    <>
      <Avtalefilter />
      <AvtaleTabell />
      {/** TODO Her kommer paginering for avtaler */}
    </>
  );
}
