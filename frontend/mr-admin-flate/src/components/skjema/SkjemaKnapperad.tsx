import { PropsWithChildren } from "react";

export function SkjemaKnapperad(props: PropsWithChildren) {
  return <div className="flex flex-row gap-2 justify-end items-start">{props.children}</div>;
}
