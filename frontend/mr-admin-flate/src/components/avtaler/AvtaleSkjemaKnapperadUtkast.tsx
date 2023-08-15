import styles from "../skjema/Skjema.module.scss";
import { Avtale, AvtaleRequest } from "mulighetsrommet-api-client";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import { UseMutationResult } from "@tanstack/react-query";

interface Props {
  utkastModus: boolean;
  mutation: UseMutationResult<Avtale, unknown, AvtaleRequest>;
}
export function AvtaleSkjemaKnapperadUtkast({ utkastModus, mutation }: Props) {
  return utkastModus ? (
    <div className={styles.button_row_utkast}>
      <OpprettAvtaleGjennomforingKnapp type="avtale" mutation={mutation} />
    </div>
  ) : null;
}
