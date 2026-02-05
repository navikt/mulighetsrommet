import type {
  EndringshistorikkDto,
  EndringshistorikkDtoNavAnsatt,
  EndringshistorikkDtoUser,
} from "@tiltaksadministrasjon/api-client";
import { formaterDatoTid } from "@mr/frontend-common/utils/date";

export interface ViewEndringshistorikkProps {
  historikk: EndringshistorikkDto;
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
            className={
              !isNavAnsatt(editedBy) ? "italic font-thin text-ax-text-neutral-subtle" : undefined
            }
            key={editedAt}
          >
            {formaterDatoTid(editedAt)} - <b>{operation}</b> - {user}
          </li>
        );
      })}
    </ul>
  );
}

function isNavAnsatt(user: EndringshistorikkDtoUser): user is EndringshistorikkDtoNavAnsatt {
  return "navIdent" in user;
}
