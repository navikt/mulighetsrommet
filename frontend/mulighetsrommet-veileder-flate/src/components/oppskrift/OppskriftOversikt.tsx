import { Alert, Skeleton } from "@navikt/ds-react";
import { Oppskrift } from "@mr/api-client-v2";
import { useOppskrifter } from "@/api/queries/useOppskrifter";
import { formaterDato } from "@/utils/Utils";
import styles from "./OppskriftOversikt.module.scss";
import { Suspense } from "react";

interface Props {
  tiltakstypeId: string;
  setOppskriftId: (id: string) => void;
}

export function OppskriftOversikt({ tiltakstypeId, setOppskriftId }: Props) {
  const { data: oppskrifter } = useOppskrifter(tiltakstypeId);

  if (!oppskrifter) return null;

  if (oppskrifter.data.length === 0) {
    return <Alert variant="info">Det er ikke lagt inn oppskrifter for denne tiltakstypen</Alert>;
  }

  return (
    <Suspense fallback={<Skeleton variant="rectangle" width="15rem" height={200} />}>
      <ul className="list-none m-0 p-0 flex gap-4 flex-1">
        {oppskrifter.data.map((oppskrift) => {
          return (
            <li
              className="bg-gray-50 rounded-[0.2rem] transition-all ease-in-out duration-350 hover:drop-shadow-[0_2px_2px_rgba(0,0,0,0.25)]"
              key={oppskrift._id}
            >
              <span role="button" onClick={() => setOppskriftId(oppskrift._id)}>
                <Oppskriftskort oppskrift={oppskrift} />
              </span>
            </li>
          );
        })}
      </ul>
    </Suspense>
  );
}

interface OppskriftKortProps {
  oppskrift: Oppskrift;
}

function Oppskriftskort({ oppskrift: { navn, beskrivelse, _updatedAt } }: OppskriftKortProps) {
  return (
    <div className="w-[15rem] p-2 flex flex-col justify-between h-full hover:cursor-pointer">
      <div>
        <h3 className="underline">{navn}</h3>
        <p>{beskrivelse}</p>
      </div>
      <small>Oppdatert: {formaterDato(new Date(_updatedAt))}</small>
    </div>
  );
}
