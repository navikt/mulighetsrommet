import { Link } from "react-router";
import css from "../root.module.css";
import { Arrangorvelger } from "./arrangorvelger/Arrangorvelger";
import { Arrangor } from "@mr/api-client";

interface Props {
  arrangorer: Arrangor[];
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
