import { PropsWithChildren } from "react";

export function KnapperadContainer(props: PropsWithChildren) {
  return (
    <div className="flex flex-row justify-end items-center [grid-area:knapperad] gap-[0.5rem]">
      {props.children}
    </div>
  );
}
