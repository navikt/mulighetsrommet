import { ArrowRightIcon } from "@navikt/aksel-icons";
import { Link } from "react-router";

type Id = string;

export interface Brodsmule {
  tittel: string;
  lenke?:
    | "/"
    | "/tiltakstyper"
    | `/tiltakstyper/${Id}`
    | "/avtaler"
    | `/avtaler/${Id}`
    | "/gjennomforinger"
    | `/gjennomforinger/${Id}`
    | "/arrangorer"
    | `/arrangorer/${Id}`;
}

interface Props {
  brodsmuler: (Brodsmule | undefined)[];
}

export function Brodsmuler({ brodsmuler }: Props) {
  const filtrerteBrodsmuler = brodsmuler.filter((b) => b !== undefined);

  return (
    <nav aria-label="Brødsmulesti" className={"bg-white pl-[0.5rem]"}>
      <ol className="flex list-none p-[0.5rem] m-0 gap-[0.5rem] flex-row">
        {filtrerteBrodsmuler.map((item, index) => {
          return (
            <li key={index}>
              {item.lenke ? (
                <div className="flex justify-center items-center gap-[0.5rem]">
                  <Link to={item.lenke}>{item.tittel}</Link>
                  <ArrowRightIcon aria-hidden="true" aria-label="Ikon for pil til høyre" />
                </div>
              ) : (
                <span aria-current="page">{item.tittel}</span>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
