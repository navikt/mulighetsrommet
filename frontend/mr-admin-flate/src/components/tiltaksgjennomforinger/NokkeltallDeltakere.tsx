import { Toggles } from "mulighetsrommet-api-client";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import { useTiltaksgjennomforingDeltakerSummary } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingDeltakerSummary";
import styles from "./NokkeltallDeltakere.module.scss";
import { HStack, HelpText } from "@navikt/ds-react";

interface Props {
  tiltaksgjennomforingId: string;
}

export function NokkeltallDeltakere({ tiltaksgjennomforingId }: Props) {
  const { data: enableDebug } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_DEBUGGER,
  );
  const { data: deltakerSummary } = useTiltaksgjennomforingDeltakerSummary(tiltaksgjennomforingId);

  if (!enableDebug) return null;

  if (!deltakerSummary) return null;

  return (
    <div className={styles.container}>
      <HStack gap="2">
        <h4 className={styles.heading}>Deltakerinformasjon</h4>
        <HelpText>Kun tilgjengelig i debug-modus</HelpText>
      </HStack>
      <dl className={styles.numbers}>
        <div className={styles.key_number}>
          <dt>Påbegynt registrering</dt>
          <dd className={styles.bold}>{deltakerSummary.pabegyntRegistrering}</dd>
        </div>
        <div className={styles.key_number}>
          <dt>Aktive</dt>
          <dd className={styles.bold}>{deltakerSummary.antallAktiveDeltakere}</dd>
        </div>
        <div className={styles.key_number}>
          <dt>Venter</dt>
          <dd className={styles.bold}>{deltakerSummary.antallDeltakereSomVenter}</dd>
        </div>
        <div className={styles.key_number}>
          <dt>Avsluttede</dt>
          <dd className={styles.bold}>{deltakerSummary.antallAvsluttedeDeltakere}</dd>
        </div>
        <div className={styles.key_number}>
          <dt>Ikke-aktuelle</dt>
          <dd className={styles.bold}>{deltakerSummary.antallIkkeAktuelleDeltakere}</dd>
        </div>
        <div className={styles.key_number}>
          <dt>Totalt</dt>
          <dd className={styles.bold}> = {deltakerSummary.antallDeltakere}</dd>
        </div>
      </dl>
    </div>
  );
}
