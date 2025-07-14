import type {
  Endringshistorikk,
  EndringshistorikkNavAnsatt,
  EndringshistorikkUser,
} from "@mr/api-client-v2";
import { formaterDatoTid } from "@mr/frontend-common/utils/date";
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
    <ul>
      {historikk.entries.map(({ operation, editedAt, editedBy }) => {
        const user = isNavAnsatt(editedBy)
          ? `${editedBy.navn} (${editedBy.navIdent})`
          : editedBy.navn;

        return (
          <li
            className={classNames({
              "italic font-thin text-text-subtle": !isNavAnsatt(editedBy),
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
