import { PropsWithChildren } from "react";

export function ToolbarButtonRow(props: PropsWithChildren) {
  return (
    <div className="flex justify-between w-full sticky top-0 z-[1] bg-[var(--ax-bg-default)] pl-2 row-start-1 col-start-2">
      {props.children}
    </div>
  );
}
