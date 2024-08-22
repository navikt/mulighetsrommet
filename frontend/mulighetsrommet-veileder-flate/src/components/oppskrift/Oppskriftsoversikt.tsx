import { Alert, Skeleton } from "@navikt/ds-react";
import { Oppskrift, Toggles } from "@mr/api-client";
import { useFeatureToggle } from "@/api/feature-toggles";
import { useOppskrifter } from "@/api/queries/useOppskrifter";
import { formaterDato } from "@/utils/Utils";
import styles from "./Oppskriftsoversikt.module.scss";
import { Suspense } from "react";

interface Props {
  tiltakstypeId: string;
  setOppskriftId: (id: string) => void;
}

export function Oppskriftsoversikt({ tiltakstypeId, setOppskriftId }: Props) {
  const { data: enableOppskrifter } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_VEILEDERFLATE_ARENA_OPPSKRIFTER,
  );

  const { data: oppskrifter } = useOppskrifter(tiltakstypeId);

  if (!enableOppskrifter) return null;

  if (!oppskrifter) return null;

  if (oppskrifter.data.length === 0) {
    return <Alert variant="info">Det er ikke lagt inn oppskrifter for denne tiltakstypen</Alert>;
  }

  return (
    <Suspense fallback={<Skeleton variant="rectangle" width="15rem" height={200} />}>
      <ul className={styles.container}>
        {oppskrifter.data.map((oppskrift) => {
          return (
            <li className={styles.item} key={oppskrift._id}>
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
    <div className={styles.kort}>
      <div>
        <h3 className={styles.tittel}>{navn}</h3>
        <p>{beskrivelse}</p>
      </div>
      <small>Oppdatert: {formaterDato(new Date(_updatedAt))}</small>
    </div>
  );
}
