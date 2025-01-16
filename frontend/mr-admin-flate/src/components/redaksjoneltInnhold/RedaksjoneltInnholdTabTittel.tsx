import { PropsWithChildren } from "react";

export function RedaksjoneltInnholdTabTittel(props: PropsWithChildren) {
  return <div className="flex flex-row items-center gap-[0.125rem]">{props.children}</div>;
}
