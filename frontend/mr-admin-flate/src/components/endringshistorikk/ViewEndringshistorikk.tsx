import type {
  Endringshistorikk,
  EndringshistorikkNavAnsatt,
  EndringshistorikkUser,
} from "@mr/api-client";
import { formaterDatoTid } from "../../utils/Utils";
import styles from "./ViewEndringshistorikk.module.scss";
import classNames from "classnames";

export interface ViewEndringshistorikkProps {
  historikk: Endringshistorikk;
}

export function ViewEndringshistorikk(props: ViewEndringshistorikkProps) {
  const { historikk } = props;

  if (historikk.entries.length === 0) {
    return <div>Endringshistorikken er tom</div>;
  }

  return (
    <ul className={styles.endringshistorikkList}>
      {historikk.entries.map(({ operation, editedAt, editedBy }) => {
        const user = isNavAnsatt(editedBy)
          ? `${editedBy.navn} (${editedBy.navIdent})`
          : editedBy.navn;

        return (
          <li
            className={classNames({
              [styles.system]: !isNavAnsatt(editedBy),
            })}
            key={editedAt}
          >
            {formaterDatoTid(editedAt)} - <b>{operation}</b> - {user}
          </li>
        );
      })}
    </ul>
  );
}

function isNavAnsatt(user: EndringshistorikkUser): user is EndringshistorikkNavAnsatt {
  return "navIdent" in user;
}
