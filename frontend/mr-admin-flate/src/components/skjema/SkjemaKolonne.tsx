import { PropsWithChildren } from "react";

export function SkjemaKolonne(props: PropsWithChildren) {
  return <div className="w-full flex flex-col">{props.children}</div>;
}
