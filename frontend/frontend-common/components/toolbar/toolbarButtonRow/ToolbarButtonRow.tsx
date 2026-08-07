import { PropsWithChildren } from "react";

export function ToolbarButtonRow(props: PropsWithChildren) {
  return (
    <div className="flex items-center justify-between w-full sticky top-0 z-[1] bg-[var(--ax-bg-default)] pl-4 row-start-1 col-start-2">
      {props.children}
    </div>
  );
}
