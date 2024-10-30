import { Link } from "@remix-run/react";
import css from "../root.module.css";
import { Arrangorvelger } from "./arrangorvelger/Arrangorvelger";

interface Props {
  arrangorer: { navn: string; organisasjonsnummer: string }[]; // TODO Bytt til modell fra OpenAPI
}

export function Header({ arrangorer }: Props) {
  return (
    <header className="bg-blue-100">
      <div className={css.side + " h-full mt-0 flex flex-row content-center items-center"}>
        <div className="flex flex-row items-center justify-between w-full ">
          <Link className="text-gray-900 font-bold text-4xl no-underline" to="/">
            Refusjonskrav
          </Link>
          <Arrangorvelger arrangorer={arrangorer} />
        </div>
      </div>
    </header>
  );
}
