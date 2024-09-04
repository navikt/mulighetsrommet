import { Link } from "@remix-run/react";
import css from "../root.module.css";

export function Header() {
  return (
    <header className="bg-blue-100 h-28">
      <div className={css.side + " h-full mt-0 flex flex-row content-center items-center"}>
        <Link className="text-gray-900 font-bold text-4xl no-underline" to="/">
          Refusjonskrav
        </Link>
      </div>
    </header>
  );
}
