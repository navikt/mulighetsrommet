import { redirect } from "react-router";
import { pathTo } from "~/utils/navigation";

export const loader = () => redirect(pathTo.utbetalinger, { status: 301 });

export default function OldOversikt() {
  return null;
}
