import { PropsWithChildren } from "react";

export function DescriptionRichtextContainer(props: PropsWithChildren) {
  return <div className="flex flex-col gap-4 mt-4">{props.children}</div>;
}
