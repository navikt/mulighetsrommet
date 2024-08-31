import { json } from "@remix-run/node";

export const loader = () => {
  return json("ok", { status: 200 });
};
